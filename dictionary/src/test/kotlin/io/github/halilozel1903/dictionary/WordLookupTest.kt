package io.github.halilozel1903.dictionary

import io.github.halilozel1903.dictionary.model.WordLookup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class WordLookupTest {

    private val hello = WordLookup(DictionaryJson.parseEntries(fixture("hello.json")))
    private val bass = WordLookup(DictionaryJson.parseEntries(fixture("bass.json")))

    @Test
    fun meaningsOfSeveralEntriesAreMergedByPartOfSpeech() {
        assertEquals("bass", bass.word)
        assertEquals(listOf("noun", "adjective"), bass.partsOfSpeech)
        val noun = bass.meaning("NOUN")!!
        assertEquals(
            listOf("The perch, Perca fluviatilis.", "A low spectrum of sound tones.", "A bass guitar."),
            noun.definitions.map { it.definition },
        )
        // "Low End" and "low end" are the same synonym.
        assertEquals(listOf("Low End"), noun.synonyms)
        assertEquals(listOf("Low End", "perch"), noun.allSynonyms)
    }

    @Test
    fun synonymsAndAntonymsOfEverySenseWithoutDuplicates() {
        assertEquals(listOf("Low End", "perch", "deep", "low"), bass.synonyms)
        assertEquals(listOf("treble", "high"), bass.antonyms)
        assertEquals(listOf("greeting", "hi", "howdy"), hello.synonyms)
        assertEquals(listOf("bye", "goodbye"), hello.antonyms)
    }

    @Test
    fun theSameRecordingIsListedOnce() {
        assertEquals(2, bass.pronunciations.size)
        // The protocol-relative recording is in both entries; its file name names no accent.
        assertEquals(listOf(null, "US"), bass.pronunciations.map { it.accent })
    }

    @Test
    fun pronunciationPrefersTheRequestedAccent() {
        assertEquals("UK", hello.pronunciation()?.accent)
        assertEquals("AU", hello.pronunciation("AU")?.accent)
        assertEquals("US", bass.pronunciation()?.accent)
        assertEquals("/beɪs/", bass.phoneticFor(bass.pronunciation()))
        val iceCream = WordLookup(DictionaryJson.parseEntries(fixture("ice-cream.json")))
        assertNull(iceCream.pronunciation())
        assertNull(iceCream.phonetic)
    }

    @Test
    fun phoneticFallsBackToThePhoneticsList() {
        assertEquals("/həˈləʊ/", hello.phonetic)
        assertEquals("/bæs/", bass.phonetic)
    }

    @Test
    fun examplesFirstDefinitionAndSources() {
        assertEquals(listOf("Hello, everyone.", "Hello? How may I help you?", "Hello! What's going on here?"), hello.examples)
        assertEquals("\"Hello!\" or an equivalent greeting.", hello.firstDefinition?.definition)
        assertEquals(5, hello.definitions.size)
        assertEquals(listOf("https://en.wiktionary.org/wiki/bass"), bass.sourceUrls)
        assertEquals(1, bass.licenses.size)
    }

    @Test
    fun aLookupNeedsAnEntry() {
        assertFailsWith<IllegalArgumentException> { WordLookup(emptyList()) }
    }
}
