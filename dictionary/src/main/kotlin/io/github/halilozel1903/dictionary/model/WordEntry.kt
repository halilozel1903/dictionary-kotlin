package io.github.halilozel1903.dictionary.model

/**
 * One entry of the Free Dictionary API: a word with one etymology. A lookup can return several entries for the
 * same spelling (for example "bass" the fish and "bass" the sound); [WordLookup] merges them.
 */
public data class WordEntry(
    /** The word as the dictionary spells it. */
    val word: String,
    /** The main phonetic transcription, for example `/ˌsɛɹ.ənˈdɪp.ɪ.ti/`, or `null`. */
    val phonetic: String?,
    /** Every transcription and recording the dictionary has for the word. */
    val phonetics: List<Phonetic>,
    /** Where the word comes from, when the dictionary says. */
    val origin: String?,
    /** The senses of the word, one [Meaning] per part of speech. */
    val meanings: List<Meaning>,
    /** The license of the entry's text (usually CC BY-SA 3.0, from Wiktionary). */
    val license: License?,
    /** Pages the entry was taken from, usually a Wiktionary page. */
    val sourceUrls: List<String>,
)

/** A transcription and, when there is one, a recording of the word. */
public data class Phonetic(
    /** The transcription, for example `/həˈləʊ/`, or `null` when there is only a recording. */
    val text: String?,
    /** An absolute https url of an MP3 recording, or `null`. */
    val audioUrl: String?,
    /** The page of the recording (usually Wikimedia Commons), or `null`. */
    val sourceUrl: String?,
    /** The recording's license, or `null`. */
    val license: License?,
) {
    /**
     * The accent of the recording, read from its file name: `US`, `UK`, `AU`, `CA` and so on, or `null` when the
     * name does not say (for example `hello-uk.mp3` is `UK`).
     */
    val accent: String?
        get() = audioUrl?.let { ACCENT.find(it.substringBefore('?'))?.groupValues?.get(1)?.uppercase() }

    /** `true` when there is a recording to play. */
    val hasAudio: Boolean get() = audioUrl != null

    private companion object {
        val ACCENT = Regex("""-([a-zA-Z]{2})\.(?:mp3|ogg|wav)$""")
    }
}

/** The senses of a word as one part of speech. */
public data class Meaning(
    /** `noun`, `verb`, `adjective`, `adverb`, `interjection` and so on, as the dictionary names it. */
    val partOfSpeech: String,
    /** The definitions, most common first. */
    val definitions: List<Definition>,
    /** Synonyms of this part of speech as a whole. */
    val synonyms: List<String>,
    /** Antonyms of this part of speech as a whole. */
    val antonyms: List<String>,
) {
    /** Synonyms of the part of speech and of each definition, without duplicates, in order. */
    val allSynonyms: List<String> get() = (synonyms + definitions.flatMap { it.synonyms }).distinctIgnoringCase()

    /** Antonyms of the part of speech and of each definition, without duplicates, in order. */
    val allAntonyms: List<String> get() = (antonyms + definitions.flatMap { it.antonyms }).distinctIgnoringCase()

    /** The example sentences of the definitions. */
    val examples: List<String> get() = definitions.mapNotNull { it.example }
}

/** One definition of a word. */
public data class Definition(
    /** The definition text. */
    val definition: String,
    /** An example sentence using the word, or `null`. */
    val example: String?,
    /** Synonyms of the word in this sense. */
    val synonyms: List<String>,
    /** Antonyms of the word in this sense. */
    val antonyms: List<String>,
)

/** A content license. */
public data class License(val name: String, val url: String?)

internal fun List<String>.distinctIgnoringCase(): List<String> {
    val seen = HashSet<String>()
    return filter { it.isNotBlank() && seen.add(it.trim().lowercase()) }.map { it.trim() }
}
