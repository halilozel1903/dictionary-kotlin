package io.github.halilozel1903.dictionary.model

/**
 * Everything the dictionary returned for one word: its [entries], plus views that merge them the way an app shows
 * a word (one phonetic, one list of meanings per part of speech, all synonyms).
 */
public data class WordLookup(val entries: List<WordEntry>) {

    init {
        require(entries.isNotEmpty()) { "A lookup has at least one entry" }
    }

    /** The word as the dictionary spells it. */
    val word: String get() = entries.first().word

    /** The first phonetic transcription of any entry, for example `/ˌsɛɹ.ənˈdɪp.ɪ.ti/`, or `null`. */
    val phonetic: String?
        get() = entries.firstNotNullOfOrNull { it.phonetic }
            ?: entries.flatMap { it.phonetics }.firstNotNullOfOrNull { it.text }

    /** Every transcription and recording, without repeating the same recording. */
    val phonetics: List<Phonetic>
        get() {
            val seenAudio = HashSet<String>()
            val seenText = HashSet<String>()
            return entries.flatMap { it.phonetics }.filter { phonetic ->
                val audio = phonetic.audioUrl
                if (audio != null) seenAudio.add(audio) else phonetic.text != null && seenText.add(phonetic.text)
            }
        }

    /** The phonetics that have a recording. */
    val pronunciations: List<Phonetic> get() = phonetics.filter { it.hasAudio }

    /**
     * The recording to play: one in a [preferred] accent (`US`, `UK`, ...) first, otherwise the first recording,
     * or `null` when the dictionary has none.
     */
    public fun pronunciation(vararg preferred: String = arrayOf("US", "UK")): Phonetic? {
        val all = pronunciations
        for (accent in preferred) {
            all.firstOrNull { it.accent.equals(accent, ignoreCase = true) }?.let { return it }
        }
        return all.firstOrNull()
    }

    /** The transcription that goes with [pronunciation], or [phonetic]. */
    public fun phoneticFor(pronunciation: Phonetic?): String? = pronunciation?.text ?: phonetic

    /**
     * The meanings of every entry merged by part of speech, in the order the parts of speech first appear. The
     * definitions of the same part of speech in different entries follow each other.
     */
    val meanings: List<Meaning>
        get() = entries.flatMap { it.meanings }
            .groupBy { it.partOfSpeech.lowercase() }
            .values
            .map { group ->
                Meaning(
                    partOfSpeech = group.first().partOfSpeech,
                    definitions = group.flatMap { it.definitions },
                    synonyms = group.flatMap { it.synonyms }.distinctIgnoringCase(),
                    antonyms = group.flatMap { it.antonyms }.distinctIgnoringCase(),
                )
            }

    /** The parts of speech, for example `[noun, verb]`. */
    val partsOfSpeech: List<String> get() = meanings.map { it.partOfSpeech }

    /** The meaning of [partOfSpeech] (case-insensitive), or `null`. */
    public fun meaning(partOfSpeech: String): Meaning? =
        meanings.firstOrNull { it.partOfSpeech.equals(partOfSpeech, ignoreCase = true) }

    /** The definitions of every part of speech. */
    val definitions: List<Definition> get() = meanings.flatMap { it.definitions }

    /** The first definition, the one most apps show under the word. */
    val firstDefinition: Definition? get() = definitions.firstOrNull()

    /** Every synonym of every sense, without duplicates. */
    val synonyms: List<String> get() = meanings.flatMap { it.allSynonyms }.distinctIgnoringCase()

    /** Every antonym of every sense, without duplicates. */
    val antonyms: List<String> get() = meanings.flatMap { it.allAntonyms }.distinctIgnoringCase()

    /** Every example sentence. */
    val examples: List<String> get() = meanings.flatMap { it.examples }

    /** The source pages of every entry, without duplicates. */
    val sourceUrls: List<String> get() = entries.flatMap { it.sourceUrls }.distinct()

    /** The licenses of the entries' text, without duplicates. */
    val licenses: List<License> get() = entries.mapNotNull { it.license }.distinct()
}
