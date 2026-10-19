package io.github.halilozel1903.dictionary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecentLookupsTest {

    @Test
    fun newestFirstWithoutDuplicates() {
        val recent = RecentLookups(maxSize = 3)
        recent.add("hello")
        recent.add("bass")
        recent.add("  Hello ")
        assertEquals(listOf("Hello", "bass"), recent.toList())
    }

    @Test
    fun keepsAtMostMaxSize() {
        val recent = RecentLookups(maxSize = 2)
        listOf("one", "two", "three").forEach(recent::add)
        assertEquals(listOf("three", "two"), recent.toList())
    }

    @Test
    fun restoresInOrderAndIgnoresBlankWords() {
        val recent = RecentLookups(maxSize = 3, restored = listOf("c", "", "b", "a", "z"))
        assertEquals(listOf("c", "b", "a"), recent.toList())
        recent.add(" ")
        recent.remove("B")
        assertEquals(listOf("c", "a"), recent.toList())
        recent.clear()
        assertEquals(0, recent.size)
    }

    @Test
    fun maxSizeMustBePositive() {
        assertFailsWith<IllegalArgumentException> { RecentLookups(maxSize = 0) }
    }
}
