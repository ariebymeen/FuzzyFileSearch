package com.fuzzyfilesearch

import com.fuzzyfilesearch.searchbox.forEachLine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for the pure helper functions used by the per-keystroke content
 * search. No IntelliJ platform dependencies here.
 */
class TextUtilsTest {

    @Test
    @DisplayName("forEachLine on empty string is a no-op")
    fun emptyText() {
        var calls = 0
        forEachLine("") { _, _, _ -> calls++; true }
        assertEquals(0, calls)
    }

    @Test
    @DisplayName("forEachLine on a single line with no trailing newline")
    fun singleLineNoNewline() {
        val seen = mutableListOf<Triple<String, Int, Int>>()
        forEachLine("hello world") { line, start, end ->
            seen.add(Triple(line, start, end))
            true
        }
        assertEquals(1, seen.size)
        assertEquals(Triple("hello world", 0, 11), seen[0])
    }

    @Test
    @DisplayName("forEachLine on a multi-line text yields one call per line")
    fun multiLine() {
        val seen = mutableListOf<Triple<String, Int, Int>>()
        forEachLine("alpha\nbeta\ngamma") { line, start, end ->
            seen.add(Triple(line, start, end))
            true
        }
        assertEquals(3, seen.size)
        assertEquals(Triple("alpha", 0, 5), seen[0])
        assertEquals(Triple("beta", 6, 10), seen[1])
        assertEquals(Triple("gamma", 11, 16), seen[2])
    }

    @Test
    @DisplayName("forEachLine on text with a trailing newline doesn't yield an empty trailing line")
    fun trailingNewline() {
        val seen = mutableListOf<Triple<String, Int, Int>>()
        forEachLine("a\nb\n") { line, start, end ->
            seen.add(Triple(line, start, end))
            true
        }
        assertEquals(2, seen.size)
        assertEquals(Triple("a", 0, 1), seen[0])
        assertEquals(Triple("b", 2, 3), seen[1])
    }

    @Test
    @DisplayName("forEachLine stops early when the block returns false")
    fun earlyStop() {
        val seen = mutableListOf<String>()
        forEachLine("a\nb\nc\nd") { line, _, _ ->
            seen.add(line)
            line != "b"
        }
        assertEquals(listOf("a", "b"), seen)
    }

    @Test
    @DisplayName("forEachLine handles consecutive blank lines correctly")
    fun consecutiveBlankLines() {
        val seen = mutableListOf<Pair<String, IntRange>>()
        forEachLine("a\n\n\nb") { line, start, end ->
            seen.add(line to (start until end))
            true
        }
        assertEquals(4, seen.size)
        assertEquals("a" to (0 until 1), seen[0])
        assertEquals("" to (2 until 2), seen[1])
        assertEquals("" to (3 until 3), seen[2])
        assertEquals("b" to (4 until 5), seen[3])
    }
}
