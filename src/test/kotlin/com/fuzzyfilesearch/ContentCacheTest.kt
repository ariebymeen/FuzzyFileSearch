package com.fuzzyfilesearch

import com.fuzzyfilesearch.searchbox.ContentCache
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.io.OutputStream

/**
 * Tests for [ContentCache]'s versioned keys. The important regression here is
 * that content read from an in-memory Document must be invalidated by the
 * Document's modification stamp, not just the VirtualFile's stamp (which only
 * changes on save).
 */
class ContentCacheTest {

    @Test
    @DisplayName("a document edit invalidates the cached text")
    fun documentStampInvalidates() {
        val cache = ContentCache(1024 * 1024)
        val vf = FakeVirtualFile("file:///tmp/a.txt", vfsStamp = 7)

        cache.put(vf, "one\ntwo", documentStamp = 1, vfsStamp = 7)

        assertEquals("one\ntwo", cache.get(vf, documentStamp = 1, vfsStamp = 7))
        assertEquals(2, cache.lineNumberAt(vf, 4, documentStamp = 1, vfsStamp = 7) ?: -1)

        // The Document changed (unsaved edit): the old text must not be served.
        assertNull(cache.get(vf, documentStamp = 2, vfsStamp = 7))
    }

    @Test
    @DisplayName("a save / external change invalidates the cached text")
    fun vfsStampInvalidates() {
        val cache = ContentCache(1024 * 1024)
        val vf = FakeVirtualFile("file:///tmp/b.txt", vfsStamp = 10)

        cache.put(vf, "disk", documentStamp = null, vfsStamp = 10)

        assertEquals("disk", cache.get(vf, documentStamp = null, vfsStamp = 10))
        assertNull(cache.get(vf, documentStamp = null, vfsStamp = 11))
    }

    @Test
    @DisplayName("invalidate drops every cached version of one file only")
    fun invalidateDropsAllVersions() {
        val cache = ContentCache(1024 * 1024)
        val a = FakeVirtualFile("file:///tmp/a.txt", vfsStamp = 1)
        val b = FakeVirtualFile("file:///tmp/b.txt", vfsStamp = 1)

        cache.put(a, "a1", documentStamp = 1, vfsStamp = 1)
        cache.put(a, "a2", documentStamp = 2, vfsStamp = 1)
        cache.put(b, "b", documentStamp = 1, vfsStamp = 1)
        assertEquals(3, cache.size())

        cache.invalidate(a)

        assertEquals(1, cache.size())
        assertNull(cache.get(a, documentStamp = 2, vfsStamp = 1))
        assertEquals("b", cache.get(b, documentStamp = 1, vfsStamp = 1))
    }

    @Test
    @DisplayName("maxBytes <= 0 disables the cache")
    fun disabledCache() {
        val cache = ContentCache(0)
        val vf = FakeVirtualFile("file:///tmp/c.txt", vfsStamp = 1)

        cache.put(vf, "text", documentStamp = 1, vfsStamp = 1)

        assertNull(cache.get(vf, documentStamp = 1, vfsStamp = 1))
        assertEquals(0, cache.size())
    }

    @Test
    @DisplayName("the least recently used entry is evicted when over budget")
    fun lruEviction() {
        // Each entry costs text.length * 2 = 8 bytes, so two entries need 16.
        val cache = ContentCache(10)
        val a = FakeVirtualFile("file:///tmp/a.txt", vfsStamp = 1)
        val b = FakeVirtualFile("file:///tmp/b.txt", vfsStamp = 1)

        cache.put(a, "aaaa", documentStamp = 1, vfsStamp = 1)
        cache.put(b, "bbbb", documentStamp = 1, vfsStamp = 1)

        assertNull(cache.get(a, documentStamp = 1, vfsStamp = 1))
        assertEquals("bbbb", cache.get(b, documentStamp = 1, vfsStamp = 1))
    }

    private class FakeVirtualFile(
        private val fileUrl: String,
        var vfsStamp: Long) : VirtualFile() {

        override fun getName(): String = fileUrl.substringAfterLast('/')
        override fun getFileSystem(): VirtualFileSystem = throw UnsupportedOperationException()
        override fun getPath(): String = fileUrl
        override fun getUrl(): String = fileUrl
        override fun isWritable(): Boolean = true
        override fun isDirectory(): Boolean = false
        override fun isValid(): Boolean = true
        override fun getParent(): VirtualFile? = null
        override fun getChildren(): Array<VirtualFile> = emptyArray()
        override fun getOutputStream(
            requestor: Any?,
            newModificationStamp: Long,
            newTimeStamp: Long): OutputStream = throw UnsupportedOperationException()
        override fun contentsToByteArray(): ByteArray = throw UnsupportedOperationException()
        override fun getTimeStamp(): Long = vfsStamp
        override fun getLength(): Long = 0
        override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {}
        override fun getInputStream(): InputStream = throw UnsupportedOperationException()
        override fun getModificationStamp(): Long = vfsStamp
    }
}
