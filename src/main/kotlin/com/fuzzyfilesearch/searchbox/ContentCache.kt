package com.fuzzyfilesearch.searchbox

import com.intellij.openapi.vfs.VirtualFile
import java.util.LinkedHashMap

/**
 * Bounded LRU cache of file contents, shared by GrepInFiles and RegexMatchInFiles
 * to avoid re-reading files from disk on every keystroke.
 *
 * Entries are keyed by:
 *  - the file URL,
 *  - the [com.intellij.openapi.editor.Document.getModificationStamp] of the
 *    file's in-memory document (when the file is open in an editor), and
 *  - the [VirtualFile.getModificationStamp] of the file itself.
 *
 * The document stamp is essential. A VirtualFile's stamp only changes when the
 * file is written to disk, while an editor Document's stamp changes on every
 * keystroke. If the cache were keyed on the VirtualFile stamp alone, a file
 * with unsaved edits would keep returning stale content until the user saved
 * it (or until the LRU evicted the entry), which is exactly the "grep results
 * don't update after I edit a file" symptom.
 *
 * Each entry stores the file text plus a precomputed IntArray of newline
 * offsets so that [lineNumberAt] is a binary search — this is what makes
 * the per-match line number lookup cheap.
 *
 * Size is bounded by total bytes, not by entry count, to behave fairly on
 * projects that mix many small files with a few very large ones. A
 * maxBytes <= 0 disables the cache and turns all get() calls into misses.
 *
 * Thread-safe: all state is guarded by this object's monitor. A plain
 * LinkedHashMap is used (instead of Collections.synchronizedMap) because the
 * trim and invalidate paths need to iterate the map atomically with the
 * counter updates.
 */
class ContentCache(initialMaxBytes: Long) {

    private data class Key(
        val url: String,
        // null when the content was read from the VFS rather than from an
        // in-memory Document.
        val documentStamp: Long?,
        val vfsStamp: Long)

    private class Entry(
        val text: String,
        // Offsets of '\n' characters in `text`, in ascending order. The line
        // containing offset `o` has line number = (number of newlines < o) + 1.
        val newlines: IntArray)

    private var maxBytes: Long = initialMaxBytes
    // Access-ordered: least-recently-used entry first. All access must happen
    // while holding this object's monitor (the methods below are synchronized).
    private val entries = LinkedHashMap<Key, Entry>(64, 0.75f, true)
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

    /**
     * Returns the cached text for [vf] at the given versions, or null on miss /
     * disabled / invalid.
     *
     * @param documentStamp [com.intellij.openapi.editor.Document.getModificationStamp]
     *        when the text was read from an in-memory document, null when it
     *        was read from the VFS.
     * @param vfsStamp the file's VFS stamp at the time the text was read. Pass
     *        the value captured before reading so a concurrent save cannot
     *        associate old text with a new stamp. Defaults to the current stamp.
     */
    @Synchronized
    fun get(vf: VirtualFile, documentStamp: Long?, vfsStamp: Long? = null): String? {
        if (maxBytes <= 0L) return null
        return entries[keyOf(vf, documentStamp, vfsStamp ?: vf.modificationStamp)]?.text
    }

    /**
     * Returns the 1-based line number containing the character at offset
     * [offset] in [vf], using the cached newline offsets, or null if the
     * matching version is not cached (cache disabled, entry evicted, or file
     * too large to cache).
     */
    @Synchronized
    fun lineNumberAt(
        vf: VirtualFile,
        offset: Int,
        documentStamp: Long?,
        vfsStamp: Long? = null): Int? {
        if (maxBytes <= 0L) return null
        val e = entries[keyOf(vf, documentStamp, vfsStamp ?: vf.modificationStamp)] ?: return null
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

    /** Insert or replace an entry. Recomputes the byte budget under the lock. */
    @Synchronized
    fun put(
        vf: VirtualFile,
        text: String,
        documentStamp: Long?,
        vfsStamp: Long? = null) {
        if (maxBytes <= 0L) return
        val key = keyOf(vf, documentStamp, vfsStamp ?: vf.modificationStamp)
        val newlines = buildNewlineOffsets(text)
        val entryBytes = text.length.toLong() * 2L + newlines.size.toLong() * 4L
        // Don't cache files bigger than the entire budget on their own.
        if (entryBytes > maxBytes) return

        val existing = entries[key]
        if (existing != null) {
            currentBytes -= bytesOf(existing)
        }
        val newEntry = Entry(text, newlines)
        entries[key] = newEntry
        currentBytes += bytesOf(newEntry)

        if (currentBytes > maxBytes) trimToSize()
    }

    /**
     * Drops every cached version of [vf], e.g. after a VFS/Document change
     * event for that file.
     */
    @Synchronized
    fun invalidate(vf: VirtualFile) {
        val url = vf.url
        val it = entries.entries.iterator()
        while (it.hasNext()) {
            val e = it.next()
            if (e.key.url == url) {
                currentBytes -= bytesOf(e.value)
                it.remove()
            }
        }
    }

    private fun bytesOf(e: Entry): Long =
        e.text.length.toLong() * 2L + e.newlines.size.toLong() * 4L

    // Must be called while holding this object's monitor.
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
    @Synchronized
    fun clear() {
        entries.clear()
        currentBytes = 0L
    }

    /** Number of cached entries. Visible for tests. */
    @Synchronized
    fun size(): Int = entries.size

    private fun keyOf(vf: VirtualFile, documentStamp: Long?, vfsStamp: Long) =
        Key(vf.url, documentStamp, vfsStamp)

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
