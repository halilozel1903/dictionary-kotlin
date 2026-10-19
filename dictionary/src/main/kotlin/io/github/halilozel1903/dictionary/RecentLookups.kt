package io.github.halilozel1903.dictionary

/**
 * The most recently looked up words, newest first, without duplicates (case-insensitive) and at most [maxSize].
 * Thread safe. Persist it with [toList] and restore it with the constructor's [restored] list (newest first), for example in SharedPreferences or
 * DataStore.
 *
 * ```kotlin
 * val recent = RecentLookups(maxSize = 10, restored = prefs.getString("recent", "")!!.lines())
 * recent.add("serendipity")
 * prefs.edit { putString("recent", recent.toList().joinToString("\n")) }
 * ```
 */
public class RecentLookups(public val maxSize: Int = DEFAULT_MAX_SIZE, restored: Iterable<String> = emptyList()) {

    init {
        require(maxSize > 0) { "maxSize must be positive, was $maxSize" }
    }

    private val lock = Any()
    private val words = ArrayList<String>()

    init {
        // Restored lists are already newest first: add oldest first so the order is kept.
        restored.toList().asReversed().forEach(::add)
    }

    /** Moves [word] to the front (adding it when new) and drops the oldest word beyond [maxSize]. Blank words are ignored. */
    public fun add(word: String) {
        val clean = word.trim().replace(Regex("\\s+"), " ")
        if (clean.isEmpty()) return
        synchronized(lock) {
            words.removeAll { it.equals(clean, ignoreCase = true) }
            words.add(0, clean)
            while (words.size > maxSize) words.removeAt(words.lastIndex)
        }
    }

    /** Removes [word] (case-insensitive). */
    public fun remove(word: String) {
        synchronized(lock) { words.removeAll { it.equals(word.trim(), ignoreCase = true) } }
    }

    /** Removes every word. */
    public fun clear() {
        synchronized(lock) { words.clear() }
    }

    /** The words, newest first. */
    public fun toList(): List<String> = synchronized(lock) { words.toList() }

    /** The number of words. */
    public val size: Int get() = synchronized(lock) { words.size }

    public companion object {
        public const val DEFAULT_MAX_SIZE: Int = 10
    }
}
