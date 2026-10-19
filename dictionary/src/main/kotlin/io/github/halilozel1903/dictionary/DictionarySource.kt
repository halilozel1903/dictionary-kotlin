package io.github.halilozel1903.dictionary

/**
 * Where [DictionaryClient] looks words up, set with [DictionaryConfig.source]. Both are free and need no key, and
 * both answer with the same [io.github.halilozel1903.dictionary.model.WordLookup] model.
 */
public enum class DictionarySource(
    /** The API root used when [DictionaryConfig.baseUrl] is not set. */
    public val defaultBaseUrl: String,
    /** A name to show to users, for example in an attribution line. */
    public val displayName: String,
) {
    /**
     * The [Free Dictionary API](https://dictionaryapi.dev) (`GET /entries/{language}/{word}`), the default: phonetics,
     * pronunciation recordings, meanings, examples, synonyms and antonyms. Its data comes from Wiktionary.
     */
    FreeDictionary("https://api.dictionaryapi.dev/api/v2/entries/", "Free Dictionary API"),

    /**
     * Wiktionary's own [REST definition endpoint](https://en.wiktionary.org/api/rest_v1/#/Page%20content/get_page_definition__term_)
     * (`GET /page/definition/{word}`): parts of speech, definitions, examples, and the synonyms and antonyms that
     * Wiktionary lists inline under a definition. It has no phonetics or recordings, so
     * [io.github.halilozel1903.dictionary.model.WordLookup.phonetic] is `null` and
     * [io.github.halilozel1903.dictionary.model.WordLookup.pronunciation] returns `null`.
     * [DictionaryConfig.language] picks the language section of the page (`en` is English). Wikimedia asks for a
     * descriptive [DictionaryConfig.userAgent] with a way to contact you; the default one names this library.
     */
    Wiktionary("https://en.wiktionary.org/api/rest_v1/page/definition/", "Wiktionary"),
}
