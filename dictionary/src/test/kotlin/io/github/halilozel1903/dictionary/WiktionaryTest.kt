package io.github.halilozel1903.dictionary

import io.github.halilozel1903.dictionary.model.LookupResult
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** [DictionarySource.Wiktionary]: Wiktionary's REST definition answers mapped into the same model. */
class WiktionaryTest {

    private val root = DictionarySource.Wiktionary.defaultBaseUrl

    private fun mockClient(requests: MutableList<String> = CopyOnWriteArrayList()): Pair<DictionaryClient, MockEngine> {
        val engine = MockEngine { request ->
            val url = request.url.toString()
            requests += url
            val json = headersOf(HttpHeaders.ContentType, "application/json")
            when (url.removePrefix(root)) {
                "happy" -> respond(fixture("wiktionary-happy.json"), HttpStatusCode.OK, json)
                "pain" -> respond(fixture("wiktionary-french-only.json"), HttpStatusCode.OK, json)
                "broken" -> respond("[]", HttpStatusCode.OK, json)
                else -> respond(fixture("wiktionary-not-found.json"), HttpStatusCode.NotFound, json)
            }
        }
        val client = DictionaryClient {
            source = DictionarySource.Wiktionary
            this.engine = engine
            retries = 0
        }
        return client to engine
    }

    @Test
    fun wiktionaryUsesItsOwnUrl() = runTest {
        val requests = CopyOnWriteArrayList<String>()
        val (client, engine) = mockClient(requests)
        client.use {
            assertEquals(DictionarySource.Wiktionary, client.source)
            assertEquals("https://en.wiktionary.org/api/rest_v1/page/definition/", client.baseUrl)
            assertEquals("https://en.wiktionary.org/api/rest_v1/page/definition/ice_cream", client.urlOf(" Ice  Cream "))
            client.lookup("Happy")
            assertEquals("https://en.wiktionary.org/api/rest_v1/page/definition/happy", requests.single())
        }
        engine.close()
    }

    @Test
    fun definitionsBecomePlainTextGroupedByPartOfSpeech() = runTest {
        val (client, engine) = mockClient()
        client.use {
            val lookup = assertIs<LookupResult.Found>(client.lookup("happy")).lookup
            assertEquals("happy", lookup.word)
            // Only the English section; parts of speech lowercased like the Free Dictionary API's.
            assertEquals(listOf("adjective", "verb"), lookup.partsOfSpeech)
            val adjective = lookup.meaning("adjective")!!.definitions
            // The empty definition is dropped.
            assertEquals(2, adjective.size)
            assertEquals(
                "Having a feeling arising from a consciousness of well-being or of enjoyment; enjoying good of any kind, " +
                    "such as comfort, peace, or tranquillity; blissful, contented, joyous.",
                adjective[0].definition,
            )
            assertEquals("Music makes her feel happy.", adjective[0].example)
            // Entities decoded, the reference left out.
            assertEquals("(obsolete) Lucky, fortunate & favoured.", adjective[1].definition)
            assertNull(adjective[1].example)
            val verb = lookup.meaning("verb")!!.definitions.single()
            assertEquals("(transitive) To make happy.", verb.definition)
            assertEquals("It happied him to hear the news.", verb.example)
        }
        engine.close()
    }

    @Test
    fun inlineSynonymsAndAntonymsAreRead() = runTest {
        val (client, engine) = mockClient()
        client.use {
            val lookup = assertIs<LookupResult.Found>(client.lookup("happy")).lookup
            // The thesaurus link is not a word.
            assertEquals(listOf("cheerful", "glad", "joyful"), lookup.synonyms)
            assertEquals(listOf("sad", "unhappy"), lookup.antonyms)
            assertEquals(listOf("cheerful", "glad", "joyful"), lookup.firstDefinition!!.synonyms)
        }
        engine.close()
    }

    @Test
    fun thereAreNoPhoneticsOrRecordings() = runTest {
        val (client, engine) = mockClient()
        client.use {
            val lookup = assertIs<LookupResult.Found>(client.lookup("happy")).lookup
            assertNull(lookup.phonetic)
            assertTrue(lookup.phonetics.isEmpty())
            assertNull(lookup.pronunciation())
            assertNull(lookup.phoneticFor(lookup.pronunciation()))
            assertEquals(listOf("https://en.wiktionary.org/wiki/happy"), lookup.sourceUrls)
            assertEquals(listOf(WiktionaryJson.LICENSE), lookup.licenses)
        }
        engine.close()
    }

    @Test
    fun aPageWithoutTheLanguageIsNotFound() = runTest {
        val (client, engine) = mockClient()
        client.use {
            val result = assertIs<LookupResult.NotFound>(client.lookup("pain"))
            assertEquals(LookupResult.NO_DEFINITIONS_FOUND, result.title)
        }
        engine.close()
    }

    @Test
    fun a404IsNotFound() = runTest {
        val (client, engine) = mockClient()
        client.use {
            val result = assertIs<LookupResult.NotFound>(client.lookup("qwzxvbn"))
            assertEquals("qwzxvbn", result.query)
            assertEquals(LookupResult.NO_DEFINITIONS_FOUND, result.title)
            assertTrue("qwzxvbn" in result.message)
        }
        engine.close()
    }

    @Test
    fun aBodyThatIsNotAnObjectIsADecodingError() = runTest {
        val (client, engine) = mockClient()
        client.use {
            assertFailsWith<DictionaryException.Decoding> { client.lookup("broken") }
        }
        engine.close()
    }

    @Test
    fun theFreeDictionaryStaysTheDefault() {
        DictionaryClient().use { client ->
            assertEquals(DictionarySource.FreeDictionary, client.source)
            assertEquals(DictionaryClient.DEFAULT_BASE_URL, client.baseUrl)
            assertEquals("https://api.dictionaryapi.dev/api/v2/entries/en/ice%20cream", client.urlOf("ice cream"))
        }
    }

    @Test
    fun nestedNymListsAndSkippedContent() {
        val parsed = WiktionaryHtml.parse(
            "Glad.<ul><li>A quotation that is not text.</li></ul><dl><dd><span class=\"nyms synonym\">" +
                "<span class=\"defdate\">Synonyms:</span> <a href=\"/wiki/merry\">merry</a>, <a href=\"/wiki/merry\">merry</a></span>" +
                "<span class=\"nyms hypernym\"><a href=\"/wiki/emotional\">emotional</a></span></dd></dl>",
        )
        assertEquals("Glad.", parsed.text)
        assertEquals(listOf("merry"), parsed.synonyms)
        assertTrue(parsed.antonyms.isEmpty())
    }
}
