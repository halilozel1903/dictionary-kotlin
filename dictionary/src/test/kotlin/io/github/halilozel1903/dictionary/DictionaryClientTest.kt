package io.github.halilozel1903.dictionary

import io.github.halilozel1903.dictionary.cache.CachePolicy
import io.github.halilozel1903.dictionary.model.LookupResult
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class DictionaryClientTest {

    @Test
    fun aKnownWordIsFound() = runTest {
        MockDictionary().use { api ->
            val result = assertIs<LookupResult.Found>(api.client.lookup("hello"))
            assertEquals("hello", result.query)
            assertEquals("hello", result.lookup.word)
            assertEquals("https://api.dictionaryapi.dev/api/v2/entries/en/hello", api.requests.single())
        }
    }

    @Test
    fun noDefinitionsFoundIsATypedResult() = runTest {
        MockDictionary().use { api ->
            val result = assertIs<LookupResult.NotFound>(api.client.lookup("qwzxvbn"))
            assertEquals("qwzxvbn", result.query)
            assertEquals(LookupResult.NO_DEFINITIONS_FOUND, result.title)
            assertNull(result.lookupOrNull)
            assertNull(api.client.lookupOrNull("qwzxvbn"))
        }
    }

    @Test
    fun wordsAreTrimmedLowercasedAndEncoded() = runTest {
        MockDictionary().use { api ->
            val result = assertIs<LookupResult.Found>(api.client.lookup("  Ice   Cream "))
            assertEquals("Ice   Cream", result.query)
            assertEquals("ice cream", result.lookup.word)
            assertEquals("https://api.dictionaryapi.dev/api/v2/entries/en/ice%20cream", api.requests.single())
            assertEquals(api.client.urlOf("ICE CREAM"), api.requests.single())
        }
    }

    @Test
    fun blankWordsFailBeforeAnyRequest() = runTest {
        MockDictionary().use { api ->
            assertFailsWith<IllegalArgumentException> { api.client.lookup("   ") }
            assertTrue(api.requests.isEmpty())
        }
    }

    @Test
    fun foundWordsAreCachedForDays() = runTest {
        MockDictionary().use { api ->
            api.client.lookup("hello")
            api.client.lookup("Hello")
            assertEquals(1, api.requests.size)
            api.clock.advance(8.days)
            api.client.lookup("hello")
            assertEquals(2, api.requests.size)
        }
    }

    @Test
    fun notFoundAnswersAreCachedForAnHour() = runTest {
        MockDictionary().use { api ->
            api.client.lookup("qwzx")
            api.clock.advance(30.minutes)
            assertIs<LookupResult.NotFound>(api.client.lookup("qwzx"))
            assertEquals(1, api.requests.size)
            api.clock.advance(1.hours)
            api.client.lookup("qwzx")
            assertEquals(2, api.requests.size)
        }
    }

    @Test
    fun offlineReturnsAnExpiredAnswer() = runTest {
        MockDictionary().use { api ->
            api.client.lookup("hello")
            api.clock.advance(30.days)
            api.offline = true
            assertIs<LookupResult.Found>(api.client.lookup("hello"))
            assertEquals(2, api.requests.size)
        }
    }

    @Test
    fun freshOnlyFailsInsteadOfReturningAnExpiredAnswer() = runTest {
        MockDictionary(cachePolicy = CachePolicy.FreshOnly).use { api ->
            api.client.lookup("hello")
            api.clock.advance(30.days)
            api.offline = true
            assertFailsWith<DictionaryException.Network> { api.client.lookup("hello") }
        }
    }

    @Test
    fun networkOnlyAlwaysRequests() = runTest {
        MockDictionary(cachePolicy = CachePolicy.NetworkOnly).use { api ->
            api.client.lookup("hello")
            api.client.lookup("hello")
            assertEquals(2, api.requests.size)
        }
    }

    @Test
    fun withoutACacheEveryLookupRequests() = runTest {
        MockDictionary(cache = null).use { api ->
            api.client.lookup("hello")
            api.client.lookup("hello")
            assertEquals(2, api.requests.size)
        }
    }

    @Test
    fun evictAndClearCache() = runTest {
        MockDictionary().use { api ->
            api.client.lookup("hello")
            api.client.evict("HELLO")
            api.client.lookup("hello")
            api.client.clearCache()
            api.client.lookup("hello")
            assertEquals(3, api.requests.size)
        }
    }

    @Test
    fun offlineWithoutCacheThrowsNetwork() = runTest {
        MockDictionary().use { api ->
            api.offline = true
            assertFailsWith<DictionaryException.Network> { api.client.lookup("hello") }
        }
    }

    @Test
    fun tooManyRequestsIsRateLimited() = runTest {
        MockDictionary().use { api ->
            api.status = HttpStatusCode.TooManyRequests
            api.retryAfter = "30"
            val error = assertFailsWith<DictionaryException.RateLimited> { api.client.lookup("hello") }
            assertEquals(30L, error.retryAfterSeconds)
        }
    }

    @Test
    fun serverErrorsAreHttp() = runTest {
        MockDictionary().use { api ->
            api.status = HttpStatusCode.BadGateway
            val error = assertFailsWith<DictionaryException.Http> { api.client.lookup("hello") }
            assertEquals(502, error.status)
        }
    }

    @Test
    fun unexpectedJsonIsDecoding() = runTest {
        MockDictionary().use { api ->
            assertFailsWith<DictionaryException.Decoding> { api.client.lookup("oops") }
        }
    }

    @Test
    fun concurrentLookupsShareOneRequest() = runTest {
        MockDictionary().use { api ->
            val results = List(5) { async { api.client.lookup("bass") } }.awaitAll()
            assertTrue(results.all { it is LookupResult.Found })
            assertEquals(1, api.requests.size)
        }
    }

    @Test
    fun aCustomTransportReplacesHttp() = runTest {
        val seen = mutableListOf<String>()
        DictionaryClient {
            transport = io.github.halilozel1903.dictionary.transport.DictionaryTransport { url ->
                seen += url
                io.github.halilozel1903.dictionary.transport.DictionaryResponse(200, fixture("hello.json"))
            }
        }.use { client ->
            assertEquals("hello", client.lookupOrNull("hello")?.word)
            assertEquals(listOf("https://api.dictionaryapi.dev/api/v2/entries/en/hello"), seen)
        }
    }

    @Test
    fun anEmptyListIsNotFound() = runTest {
        DictionaryClient {
            transport = io.github.halilozel1903.dictionary.transport.DictionaryTransport {
                io.github.halilozel1903.dictionary.transport.DictionaryResponse(200, "[]")
            }
        }.use { client ->
            assertIs<LookupResult.NotFound>(client.lookup("nothing"))
        }
    }
}
