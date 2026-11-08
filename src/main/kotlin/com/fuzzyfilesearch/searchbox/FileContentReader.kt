package com.fuzzyfilesearch.searchbox

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.vfs.VirtualFile

/**
 * Text loaded for a file, together with the version stamps that identify it in
 * [ContentCache].
 *
 * @param documentStamp the in-memory document's modification stamp, or null
 *        when the text was read from the VFS/disk.
 * @param vfsStamp the VirtualFile modification stamp captured before reading.
 * @param fromCache true when this text was served by [ContentCache].
 */
class FileText(
    val text: String,
    val documentStamp: Long?,
    val vfsStamp: Long,
    val fromCache: Boolean)

/**
 * Loads the current text of [virtualFile], preferring the in-memory editor
 * Document over the on-disk content.
 *
 * This is the key difference from the previous implementation: grepping used
 * to read [VirtualFile.getInputStream], i.e. the bytes cached by the VFS. Those
 * bytes only change when the file is saved, so unsaved edits were invisible to
 * grep (and the old ContentCache, keyed only on the VFS stamp, kept returning
 * the same text indefinitely).
 *
 * [FileDocumentManager.getCachedDocument] is used instead of `getDocument` so
 * we only read documents that the IDE already has in memory (open editors and
 * the search popup's preview) and don't force every file in the project to be
 * loaded into a Document just to search it.
 */
fun loadFileText(
    cache: ContentCache,
    virtualFile: VirtualFile,
    printErrors: Boolean = false): FileText? {
    // Capture the VFS stamp once. It must identify both the cache lookup and
    // the eventual insertion, otherwise a concurrent save could associate the
    // text we read with a newer stamp than the content it came from.
    val vfsStamp = try {
        virtualFile.modificationStamp
    } catch (e: Throwable) {
        if (printErrors) {
            println("Error reading file ${virtualFile.name}: ${e.message}")
        }
        return null
    }

    val document = try {
        ReadAction.compute<Document?, Throwable> {
            FileDocumentManager.getInstance().getCachedDocument(virtualFile)
        }
    } catch (e: ProcessCanceledException) {
        throw e
    } catch (e: Throwable) {
        if (printErrors) {
            println("FuzzyFileSearch: no document for ${virtualFile.name}: ${e.message}")
        }
        null
    }

    if (document != null) {
        val documentStamp = document.modificationStamp
        cache.get(virtualFile, documentStamp, vfsStamp)?.let {
            return FileText(it, documentStamp, vfsStamp, true)
        }

        val text = try {
            // The document text includes unsaved edits. The read action keeps
            // the document from being modified concurrently while we copy it.
            ReadAction.compute<String, Throwable> { document.text }
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Throwable) {
            if (printErrors) {
                println("Error reading document ${virtualFile.name}: ${e.message}")
            }
            null
        }
        if (text != null) {
            cache.put(virtualFile, text, documentStamp, vfsStamp)
            return FileText(text, documentStamp, vfsStamp, false)
        }
    }

    // No live document (or it could not be read): fall back to the VFS copy.
    cache.get(virtualFile, null, vfsStamp)?.let {
        return FileText(it, null, vfsStamp, true)
    }
    val text = readVirtualFileText(virtualFile, printErrors) ?: return null
    cache.put(virtualFile, text, null, vfsStamp)
    return FileText(text, null, vfsStamp, false)
}

/**
 * Reads the on-disk content of [virtualFile] through the VFS.
 *
 * The exists()/isValid() check is necessary but not sufficient during project
 * open / indexing: the VFS can mark a file valid while the underlying file has
 * been deleted or is locked. The actual stream open + read can still throw
 * IOException, IllegalArgumentException, or AssertionError, so the whole read
 * is guarded and null is returned on failure.
 */
private fun readVirtualFileText(virtualFile: VirtualFile, printErrors: Boolean): String? {
    if (!virtualFile.exists() || !virtualFile.isValid) {
        if (printErrors) {
            println("Error reading file ${virtualFile.name}: not valid or does not exist")
        }
        return null
    }
    return try {
        virtualFile.inputStream.bufferedReader().use { it.readText() }
    } catch (e: ProcessCanceledException) {
        throw e
    } catch (e: Throwable) {
        if (printErrors) {
            println("Error reading file ${virtualFile.name}: ${e.message}")
        }
        null
    }
}
