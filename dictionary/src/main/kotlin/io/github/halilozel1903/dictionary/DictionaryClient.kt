package io.github.halilozel1903.dictionary

import io.github.halilozel1903.dictionary.cache.CachePolicy
import io.github.halilozel1903.dictionary.cache.DictionaryCache
import io.github.halilozel1903.dictionary.model.LookupResult
import io.github.halilozel1903.dictionary.model.WordLookup
import io.github.halilozel1903.dictionary.transport.DictionaryResponse
import io.github.halilozel1903.dictionary.transport.DictionaryTransport
import io.github.halilozel1903.dictionary.transport.KtorTransport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

/**
 * A coroutine client for the [Free Dictionary API](https://dictionaryapi.dev): English words with phonetics,
 * pronunciation recordings, meanings by part of speech, definitions, examples, synonyms and antonyms. No API key.
 * Set [DictionaryConfig.source] to [DictionarySource.Wiktionary] to read Wiktionary's REST API instead, into the
 * same model (without phonetics and recordings).
 *
 * ```kotlin
 * val dictionary = DictionaryClient()
 * when (val result = dictionary.lookup("serendipity")) {
 *     is LookupResult.Found -> {
 *         val word = result.lookup
 *         word.phonetic                      // "/ˌsɛɹ.ənˈdɪp.ɪ.ti/"
 *         word.pronunciation()?.audioUrl     // an https MP3 url, US accent first
 *         word.meanings.first().definitions  // noun definitions
 *         word.synonyms
 *     }
 *     is LookupResult.NotFound -> println(result.title)   // "No Definitions Found"
 * }
 * ```
 *
 * Every function is main-safe. A word the dictionary does not know is a [LookupResult.NotFound], not an
 * exception; other failures throw a [DictionaryException]. Answers (found and not found) are cached by url, see
 * [DictionaryConfig.cache], and identical lookups running at the same time share one network call. Close the
 * client when you no longer need it.
 */
public class DictionaryClient(configure: DictionaryConfig.() -> Unit = {}) : AutoCloseable {

    private val config = DictionaryConfig().apply(configure)

    /** Where words are looked up. */
    public val source: DictionarySource = config.source

    /** The API root, always ending with a slash. */
    public val baseUrl: String = (config.baseUrl ?: source.defaultBaseUrl).let { if (it.endsWith('/')) it else "$it/" }

    /** The language code used in urls. */
    public val language: String = config.language.trim().lowercase(Locale.ROOT).also {
        require(it.isNotEmpty() && it.all { c -> c.isLetter() || c == '-' || c == '_' }) { "Invalid language \"${config.language}\"" }
    }

    private val transport: DictionaryTransport = config.transport ?: KtorTransport(
        httpClient = config.httpClient,
        engine = config.engine,
        userAgent = config.userAgent,
        requestTimeoutMillis = config.requestTimeoutMillis,
        retries = config.retries,
    )
    private val cache: DictionaryCache? = config.cache.takeIf { config.cachePolicy != CachePolicy.NetworkOnly }
    private val inFlightLock = Mutex()
    private val inFlight = HashMap<String, CompletableDeferred<DictionaryResponse>>()

    /**
     * `GET /entries/{language}/{word}` (or Wiktionary's `GET /page/definition/{word}`): looks [word] up. Returns
     * [LookupResult.Found] with every entry, or [LookupResult.NotFound] when the API answers 404 "No Definitions
     * Found" (Wiktionary: 404, or a page without definitions in [language]).
     *
     * The word is trimmed, inner whitespace is collapsed and it is lowercased for the request (the dictionary is
     * case-insensitive), so "Hello " and "hello" share one cache entry. Phrases like "ice cream" work.
     *
     * @throws IllegalArgumentException when [word] is blank.
     * @throws DictionaryException when the request fails or the answer cannot be read.
     */
    public suspend fun lookup(word: String): LookupResult {
        val query = normalize(word)
        val url = url(query)
        val cached = cache?.get(url)
        val now = config.clock.millis()
        if (cached != null && now - cached.storedAtMillis in 0 until ttl(cached.status)) {
            runCatching { decode(word.trim(), url, cached.status, cached.body) }.getOrNull()?.let { return it }
        }
        val response = try {
            loadShared(url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: DictionaryException) {
            // Offline or the API is down: an older answer beats none.
            val stale = e is DictionaryException.Network || e is DictionaryException.Http || e is DictionaryException.RateLimited
            if (stale && cached != null && config.cachePolicy == CachePolicy.CacheFirst) {
                runCatching { decode(word.trim(), url, cached.status, cached.body) }.getOrNull()?.let { return it }
            }
            throw e
        }
        val result = decode(word.trim(), url, response.status, response.body)
        cache?.put(url, DictionaryCache.Entry(response.body, response.status, config.clock.millis()))
        return result
    }

    /** [lookup] for [word], or `null` when the dictionary does not know it. */
    public suspend fun lookupOrNull(word: String): WordLookup? = lookup(word).lookupOrNull

    /** The request url of [word], for logging or sharing. */
    public fun urlOf(word: String): String = url(normalize(word))

    /** Removes the cached answer of [word]. */
    public suspend fun evict(word: String) {
        config.cache?.remove(urlOf(word))
    }

    /** Removes every cached answer. */
    public suspend fun clearCache() {
        config.cache?.clear()
    }

    /** Releases the HTTP client this instance created. */
    override fun close() {
        (transport as? AutoCloseable)?.close()
    }

    private fun url(query: String): String = when (source) {
        DictionarySource.FreeDictionary -> baseUrl + language + "/" + URLEncoder.encode(query, Charsets.UTF_8).replace("+", "%20")
        DictionarySource.Wiktionary -> baseUrl + WiktionaryJson.title(query)
    }

    private fun ttl(status: Int): Long =
        (if (status == HTTP_NOT_FOUND) config.notFoundTtl else config.foundTtl).inWholeMilliseconds

    /** Maps an answer to a result. 404 and an empty list are "not found"; other errors throw. */
    private fun decode(query: String, url: String, status: Int, body: String): LookupResult = when (status) {
        in 200..299 -> {
            val entries = try {
                when (source) {
                    DictionarySource.FreeDictionary -> DictionaryJson.parseEntries(body)
                    DictionarySource.Wiktionary -> WiktionaryJson.parseEntries(normalize(query), language, body)
                }
            } catch (e: Exception) {
                throw DictionaryException.Decoding(url, e)
            }
            if (entries.isEmpty()) notFound(query, null) else LookupResult.Found(query, WordLookup(entries))
        }
        HTTP_NOT_FOUND -> notFound(query, body)
        else -> throw DictionaryException.Http(url, status)
    }

    private fun notFound(query: String, body: String?): LookupResult.NotFound = when (source) {
        DictionarySource.FreeDictionary -> DictionaryJson.parseNotFound(query, body)
        // Wiktionary's 404 body is a generic "Not found." problem; say what it means for a word instead.
        DictionarySource.Wiktionary -> LookupResult.NotFound(
            query = query,
            title = LookupResult.NO_DEFINITIONS_FOUND,
            message = "Wiktionary has no definitions of \"$query\" in this language.",
            resolution = null,
        )
    }

    /** Joins a request for the same url that is already running, or starts one. */
    private suspend fun loadShared(url: String): DictionaryResponse {
        var owner = false
        val deferred = inFlightLock.withLock {
            inFlight[url] ?: CompletableDeferred<DictionaryResponse>().also {
                inFlight[url] = it
                owner = true
            }
        }
        if (!owner) {
            return try {
                deferred.await()
            } catch (e: CancellationException) {
                // The coroutine that made the request was cancelled, not this one: try again.
                currentCoroutineContext().ensureActive()
                loadShared(url)
            }
        }
        val result = try {
            Result.success(request(url))
        } catch (e: Throwable) {
            Result.failure(e)
        }
        withContext(NonCancellable) { inFlightLock.withLock { inFlight.remove(url) } }
        result.fold(
            onSuccess = { deferred.complete(it) },
            onFailure = { deferred.completeExceptionally(it) },
        )
        return result.getOrThrow()
    }

    private suspend fun request(url: String): DictionaryResponse {
        val response = try {
            transport.get(url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: DictionaryException) {
            throw e
        } catch (e: Exception) {
            throw DictionaryException.Network(url, e)
        }
        return when {
            response.status in 200..299 || response.status == HTTP_NOT_FOUND -> response
            response.status == HTTP_TOO_MANY_REQUESTS -> throw DictionaryException.RateLimited(url, response.retryAfterSeconds)
            else -> throw DictionaryException.Http(url, response.status)
        }
    }

    public companion object {
        /** `https://api.dictionaryapi.dev/api/v2/entries/`, the root of [DictionarySource.FreeDictionary]. */
        public const val DEFAULT_BASE_URL: String = "https://api.dictionaryapi.dev/api/v2/entries/"

        /** English, the language the Free Dictionary API serves. */
        public const val ENGLISH: String = "en"

        private const val HTTP_NOT_FOUND = 404
        private const val HTTP_TOO_MANY_REQUESTS = 429

        /** Trims [word], collapses inner whitespace and lowercases it; fails when it is blank. */
        internal fun normalize(word: String): String {
            val query = word.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
            require(query.isNotEmpty()) { "The word is blank" }
            return query
        }
    }
}
