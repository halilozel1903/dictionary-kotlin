package io.github.halilozel1903.dictionary.sample

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import io.github.halilozel1903.dictionary.RecentLookups

/** The recent lookups, kept in SharedPreferences with the library's [RecentLookups] and observable from Compose. */
class RecentStore(context: Context) {
    private val prefs = context.getSharedPreferences("recent", Context.MODE_PRIVATE)
    private val recent = RecentLookups(maxSize = 8, restored = prefs.getString(KEY, "").orEmpty().lines())

    /** The words, newest first. */
    val words = mutableStateListOf<String>().apply { addAll(recent.toList()) }

    fun add(word: String) {
        recent.add(word)
        save()
    }

    fun clear() {
        recent.clear()
        save()
    }

    private fun save() {
        val list = recent.toList()
        prefs.edit().putString(KEY, list.joinToString("\n")).apply()
        words.clear()
        words.addAll(list)
    }

    private companion object {
        const val KEY = "words"
    }
}
