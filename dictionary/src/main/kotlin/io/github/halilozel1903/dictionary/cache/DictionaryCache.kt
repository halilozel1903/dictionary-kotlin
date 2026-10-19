package io.github.halilozel1903.dictionary.cache

/**
 * Stores lookup answers by request url, with their HTTP status and the time they were stored.
 * [io.github.halilozel1903.dictionary.DictionaryClient] decides how old an entry may be: days for a word it found,
 * an hour for a word it did not.
 *
 * Implement it to keep answers elsewhere (a file, a database, DataStore) so looked up words open offline.
 */
public interface DictionaryCache {
    /** The entry stored for [key], or `null`. */
    public suspend fun get(key: String): Entry?

    /** Stores [entry] for [key]. */
    public suspend fun put(key: String, entry: Entry)

    /** Removes the entry of [key]. */
    public suspend fun remove(key: String)

    /** Removes every entry. */
    public suspend fun clear()

    /** A response body, its HTTP status (200 or 404) and when it was stored, in epoch milliseconds. */
    public data class Entry(val body: String, val status: Int, val storedAtMillis: Long)

    public companion object {
        /** An in-memory LRU cache of up to [maxEntries] answers. */
        public fun memory(maxEntries: Int = MemoryCache.DEFAULT_MAX_ENTRIES): MemoryCache = MemoryCache(maxEntries)
    }
}

/** How [io.github.halilozel1903.dictionary.DictionaryClient] uses its cache. */
public enum class CachePolicy {
    /**
     * Use a fresh cached answer when there is one, otherwise request it and cache it. When the request fails and an
     * older entry exists, return that instead of failing (offline). The default.
     */
    CacheFirst,

    /** Like [CacheFirst], but fail instead of returning an expired entry. */
    FreshOnly,

    /** Always request, and never read or write the cache. */
    NetworkOnly,
}
