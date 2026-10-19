package io.github.halilozel1903.dictionary.cache

/** A thread safe, least recently used, in-memory [DictionaryCache]. */
public class MemoryCache(public val maxEntries: Int = DEFAULT_MAX_ENTRIES) : DictionaryCache {

    init {
        require(maxEntries > 0) { "maxEntries must be positive, was $maxEntries" }
    }

    private val lock = Any()
    private val entries = object : LinkedHashMap<String, DictionaryCache.Entry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, DictionaryCache.Entry>?): Boolean =
            size > maxEntries
    }

    /** The number of stored entries. */
    public val size: Int get() = synchronized(lock) { entries.size }

    override suspend fun get(key: String): DictionaryCache.Entry? = synchronized(lock) { entries[key] }

    override suspend fun put(key: String, entry: DictionaryCache.Entry) {
        synchronized(lock) { entries[key] = entry }
    }

    override suspend fun remove(key: String) {
        synchronized(lock) { entries.remove(key) }
    }

    override suspend fun clear() {
        synchronized(lock) { entries.clear() }
    }

    public companion object {
        /** An answer is a few KB (a long word like "set" is about 100 KB). */
        public const val DEFAULT_MAX_ENTRIES: Int = 64
    }
}
