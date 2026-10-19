package io.github.halilozel1903.dictionary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DictionaryJsonTest {

    @Test
    fun parsesAnEntryWithEveryField() {
        val entry = DictionaryJson.parseEntries(fixture("hello.json")).single()
        assertEquals("hello", entry.word)
        assertEquals("/həˈləʊ/", entry.phonetic)
        assertEquals(listOf("noun", "verb", "interjection"), entry.meanings.map { it.partOfSpeech })
        assertEquals("CC BY-SA 3.0", entry.license?.name)
        assertEquals(listOf("https://en.wiktionary.org/wiki/hello"), entry.sourceUrls)
        val interjection = entry.meanings[2]
        assertEquals(3, interjection.definitions.size)
        assertEquals("Hello, everyone.", interjection.definitions[0].example)
        assertEquals(listOf("hi", "howdy", "Hi"), interjection.synonyms)
        assertEquals(listOf("bye", "goodbye"), interjection.antonyms)
    }

    @Test
    fun phoneticsKeepAudioAndDropEmptyUrls() {
        val phonetics = DictionaryJson.parseEntries(fixture("hello.json")).single().phonetics
        assertEquals(3, phonetics.size)
        // Audio only, no transcription.
        assertNull(phonetics[0].text)
        assertEquals("AU", phonetics[0].accent)
        assertEquals("BY-SA 4.0", phonetics[0].license?.name)
        assertEquals("UK", phonetics[1].accent)
        assertEquals("https://commons.wikimedia.org/w/index.php?curid=9021983", phonetics[1].sourceUrl)
        // "audio": "" means no recording.
        assertEquals("/həˈloʊ/", phonetics[2].text)
        assertNull(phonetics[2].audioUrl)
        assertNull(phonetics[2].accent)
    }

    @Test
    fun protocolRelativeAudioGetsHttps() {
        val entry = DictionaryJson.parseEntries(fixture("bass.json")).first()
        assertEquals("https://ssl.gstatic.com/dictionary/static/sounds/20200429/bass--_gb_1.mp3", entry.phonetics.single().audioUrl)
        assertEquals("Old English bærs.", entry.origin)
    }

    @Test
    fun missingFieldsBecomeEmpty() {
        val entry = DictionaryJson.parseEntries(fixture("ice-cream.json")).single()
        assertNull(entry.phonetic)
        assertTrue(entry.phonetics.isEmpty())
        assertNull(entry.license)
        assertNull(entry.meanings.single().definitions.single().example)
    }

    @Test
    fun parsesTheNotFoundBody() {
        val notFound = DictionaryJson.parseNotFound("qwzx", fixture("not-found.json"))
        assertEquals("qwzx", notFound.query)
        assertEquals("No Definitions Found", notFound.title)
        assertTrue(notFound.message.startsWith("Sorry pal"))
        assertTrue(notFound.resolution!!.contains("search again"))
    }

    @Test
    fun anEmptyNotFoundBodyGetsTheUsualTitle() {
        val notFound = DictionaryJson.parseNotFound("qwzx", "")
        assertEquals("No Definitions Found", notFound.title)
        assertNull(notFound.resolution)
    }

    @Test
    fun anObjectInsteadOfAnArrayFails() {
        assertFails { DictionaryJson.parseEntries(fixture("malformed.json")) }
        assertFails { DictionaryJson.parseEntries("[{\"phonetics\":[]}]") }
    }
}
