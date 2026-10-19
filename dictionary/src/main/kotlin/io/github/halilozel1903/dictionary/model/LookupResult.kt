package io.github.halilozel1903.dictionary.model

/**
 * The answer to a lookup: the word was [Found], or the dictionary has [NotFound] it. Other failures (offline,
 * rate limited, server errors) are thrown as [io.github.halilozel1903.dictionary.DictionaryException].
 *
 * ```kotlin
 * when (val result = dictionary.lookup("serendipity")) {
 *     is LookupResult.Found -> show(result.lookup)
 *     is LookupResult.NotFound -> showMessage(result.title)   // "No Definitions Found"
 * }
 * ```
 */
public sealed interface LookupResult {
    /** The word that was looked up, as passed in (trimmed). */
    public val query: String

    /** The dictionary knows the word. */
    public data class Found(override val query: String, val lookup: WordLookup) : LookupResult

    /**
     * The dictionary has no entry for [query]: HTTP 404 with the API's `{"title":"No Definitions Found", ...}` body.
     * [title], [message] and [resolution] are the API's own texts.
     */
    public data class NotFound(
        override val query: String,
        val title: String,
        val message: String,
        val resolution: String?,
    ) : LookupResult

    /** The lookup when [Found], otherwise `null`. */
    public val lookupOrNull: WordLookup? get() = (this as? Found)?.lookup

    public companion object {
        /** The API's title of a 404 answer. */
        public const val NO_DEFINITIONS_FOUND: String = "No Definitions Found"
    }
}
