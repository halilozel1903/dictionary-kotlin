package io.github.halilozel1903.dictionary

import io.github.halilozel1903.dictionary.cache.CachePolicy
import io.github.halilozel1903.dictionary.cache.DictionaryCache
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration

/**
 * Reads `src/test/resources/fixtures/<name>`. The fixtures are short, hand-written answers in the Free Dictionary
 * API's response format (trimmed entries, not full recordings); they are used by these unit tests only.
 */
internal fun fixture(name: String): String =
    requireNotNull(Fixtures::class.java.getResource("/fixtures/$name")) { "No fixture $name" }.readText()

internal object Fixtures {
    /** The word part of the url (after `/entries/en/`) to fixture file. */
    val routes: Map<String, String> = mapOf(
        "hello" to "hello.json",
        "bass" to "bass.json",
        "ice%20cream" to "ice-cream.json",
        "oops" to "malformed.json",
    )
}

/** A clock the tests move by hand. */
internal class TestClock(var instant: Instant = Instant.parse("2026-10-14T12:00:00Z")) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId?): Clock = this
    override fun instant(): Instant = instant

    fun advance(duration: Duration) {
        instant = instant.plusMillis(duration.inWholeMilliseconds)
    }
}

/**
 * A client on a Ktor [MockEngine] that serves the fixtures by [Fixtures.routes] and answers 404 with the API's
 * "No Definitions Found" body for anything else. [offline] fails every request; [status] answers every request
 * with that status.
 */
internal class MockDictionary(
    cache: DictionaryCache? = DictionaryCache.memory(),
    cachePolicy: CachePolicy = CachePolicy.CacheFirst,
) : AutoCloseable {
    val clock = TestClock()
    val requests: MutableList<String> = CopyOnWriteArrayList()
    var offline = false
    var status: HttpStatusCode? = null
    var retryAfter: String? = null

    private val root = DictionaryClient.DEFAULT_BASE_URL + "en/"

    val engine = MockEngine { request ->
        val url = request.url.toString()
        requests += url
        if (offline) throw java.io.IOException("offline")
        status?.let { code ->
            val headers = retryAfter?.let { headersOf(HttpHeaders.RetryAfter, it) } ?: headersOf()
            return@MockEngine respond("""{"message":"error"}""", code, headers)
        }
        val json = headersOf(HttpHeaders.ContentType, "application/json")
        when (val name = Fixtures.routes[url.removePrefix(root)]) {
            null -> respond(fixture("not-found.json"), HttpStatusCode.NotFound, json)
            else -> respond(fixture(name), HttpStatusCode.OK, json)
        }
    }

    val client = DictionaryClient {
        engine = this@MockDictionary.engine
        this.cache = cache
        this.cachePolicy = cachePolicy
        clock = this@MockDictionary.clock
        retries = 0
    }

    override fun close() {
        client.close()
        engine.close()
    }
}
