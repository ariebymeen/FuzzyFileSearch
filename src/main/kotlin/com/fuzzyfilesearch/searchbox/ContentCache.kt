package com.fuzzyfilesearch.searchbox

import com.intellij.openapi.vfs.VirtualFile
import java.util.Collections

/**
 * Bounded LRU cache of file contents, shared by GrepInFiles and RegexMatchInFiles
 * to avoid re-reading files from disk on every keystroke.
 *
 * Entries are keyed by (vf.url, vf.modificationStamp). When the file's stamp
 * changes (e.g. after an edit) the entry is automatically invalidated on the
 * next lookup.
 *
 * Each entry stores the file text plus a precomputed IntArray of newline
 * offsets so that [lineNumberAt] is a binary search — this is what makes
 * the per-match line number lookup cheap.
 *
 * Size is bounded by total bytes, not by entry count, to behave fairly on
 * projects that mix many small files with a few very large ones. A
 * maxBytes <= 0 disables the cache and turns all get() calls into misses.
 *
 * Thread-safe: backing map is a synchronized LinkedHashMap with accessOrder=true.
 */
class ContentCache(initialMaxBytes: Long) {

    private data class Key(val url: String, val stamp: Long)

    private class Entry(
        val text: String,
        // Offsets of '\n' characters in `text`, in ascending order. The line
        // containing offset `o` has line number = (number of newlines < o) + 1.
        val newlines: IntArray)

    private var maxBytes: Long = initialMaxBytes
    private val entries: MutableMap<Key, Entry> =
        Collections.synchronizedMap(object : LinkedHashMap<Key, Entry>(64, 0.75f, true) {})
    private var currentBytes: Long = 0L

    /** Resize the cache. Drops the entire cache if the new size is smaller than
     *  the current usage — callers can rebuild on demand. */
    @Synchronized
    fun setMaxBytes(newMaxBytes: Long) {
        maxBytes = newMaxBytes
        if (maxBytes <= 0L || currentBytes > maxBytes) {
            entries.clear()
            currentBytes = 0L
        }
    }

    /** Returns the cached text for [vf], or null on miss / disabled / invalid. */
    fun get(vf: VirtualFile): String? {
        if (maxBytes <= 0L) return null
        return entries[keyOf(vf)]?.text
    }

    /**
     * Returns the 1-based line number containing the character at byte offset
     * [offset] in [vf], using the cached newline offsets. Returns 1 if the
     * file is not cached.
     */
    fun lineNumberAt(vf: VirtualFile, offset: Int): Int {
        if (maxBytes <= 0L) return 1
        val e = entries[keyOf(vf)] ?: return 1
        val newlines = e.newlines
        // Binary search for the index of the first newline strictly > offset.
        // That index equals the number of newlines strictly before `offset`.
        var lo = 0
        var hi = newlines.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (newlines[mid] < offset) lo = mid + 1 else hi = mid
        }
        return lo + 1
    }

    /** Insert or replace an entry. Recomputes the byte budget under the map's lock. */
    fun put(vf: VirtualFile, text: String) {
        if (maxBytes <= 0L) return
        val key = keyOf(vf)
        val newlines = buildNewlineOffsets(text)
        val entryBytes = text.length.toLong() * 2L + newlines.size.toLong() * 4L
        // Don't cache files bigger than the entire budget on their own.
        if (entryBytes > maxBytes) return

        synchronized(this) {
            val existing = entries[key]
            if (existing != null) {
                currentBytes -= bytesOf(existing)
            }
            val newEntry = Entry(text, newlines)
            entries[key] = newEntry
            currentBytes += bytesOf(newEntry)

            if (currentBytes > maxBytes) trimToSize()
        }
    }

    private fun bytesOf(e: Entry): Long =
        e.text.length.toLong() * 2L + e.newlines.size.toLong() * 4L

    private fun trimToSize() {
        // Iteration order with accessOrder=true is least-recently-accessed first.
        val it = entries.entries.iterator()
        while (it.hasNext() && currentBytes > maxBytes) {
            val e = it.next()
            currentBytes -= bytesOf(e.value)
            it.remove()
        }
    }

    /** Drop all entries. */
    fun clear() {
        entries.clear()
        currentBytes = 0L
    }

    /** Number of cached entries. Visible for tests. */
    fun size(): Int = entries.size

    private fun keyOf(vf: VirtualFile) = Key(vf.url, vf.modificationStamp)

    private fun buildNewlineOffsets(text: String): IntArray {
        // Count newlines first to size the IntArray exactly.
        var count = 0
        for (i in text.indices) if (text[i] == '\n') count++
        if (count == 0) return IntArray(0)
        val out = IntArray(count)
        var k = 0
        for (i in text.indices) {
            if (text[i] == '\n') {
                out[k++] = i
            }
        }
        return out
    }
}
