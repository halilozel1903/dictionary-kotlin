package io.github.halilozel1903.dictionary

import io.github.halilozel1903.dictionary.cache.CachePolicy
import io.github.halilozel1903.dictionary.cache.DictionaryCache
import io.github.halilozel1903.dictionary.transport.DictionaryTransport
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/** Settings for [DictionaryClient], set in its builder block. */
public class DictionaryConfig internal constructor() {
    /** The API root, [DictionaryClient.DEFAULT_BASE_URL] (`https://api.dictionaryapi.dev/api/v2/entries/`). */
    public var baseUrl: String = DictionaryClient.DEFAULT_BASE_URL

    /** The language code in the url. The Free Dictionary API serves English (`en`). */
    public var language: String = DictionaryClient.ENGLISH

    /**
     * The Ktor engine. `null` uses CIO. Pass `OkHttp.create()` (add `io.ktor:ktor-client-okhttp`) to share your
     * app's OkHttp stack, or a `MockEngine` in tests. The client does not close an engine you pass.
     */
    public var engine: HttpClientEngine? = null

    /** A ready Ktor client to use as is, instead of [engine]. [requestTimeoutMillis] and [retries] then do not apply. */
    public var httpClient: HttpClient? = null

    /** Replaces HTTP entirely, for example to serve recorded responses in tests. */
    public var transport: DictionaryTransport? = null

    /** Where answers are cached. An in-memory LRU cache by default; `null` turns caching off. */
    public var cache: DictionaryCache? = DictionaryCache.memory()

    /** How the cache is used. */
    public var cachePolicy: CachePolicy = CachePolicy.CacheFirst

    /** How long a found word stays fresh in the cache. Dictionary entries rarely change. */
    public var foundTtl: Duration = 7.days

    /** How long a "No Definitions Found" answer stays fresh, so a typo is not requested again and again. */
    public var notFoundTtl: Duration = 1.hours

    /** Cache ages are measured with this clock. */
    public var clock: Clock = Clock.systemUTC()

    /** Sent with every request. */
    public var userAgent: String = "dictionary-kotlin/1.0.0 (+https://github.com/halilozel1903/dictionary-kotlin)"

    /** Timeout of one request, in milliseconds. */
    public var requestTimeoutMillis: Long = 20_000

    /** Retries on network errors and 5xx responses, with exponential backoff. */
    public var retries: Int = 2
}
