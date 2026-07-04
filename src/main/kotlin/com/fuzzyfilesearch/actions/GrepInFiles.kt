package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.renderers.HighlightedStringCellRenderer
import com.fuzzyfilesearch.renderers.StringMatchInstanceItem
import com.fuzzyfilesearch.searchbox.*
import com.fuzzyfilesearch.settings.GlobalSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Collections
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

class History(val query: String, val timeMs: Long)

class GrepInFiles(
    val actionSettings: utils.ActionSettings,
    val globalSettings: GlobalSettings.SettingsState) : AnAction(actionSettings.name) {

    data class Settings(
        val path: String,
        val extensionList: List<String>,
        var onlyVcsTracked: Boolean)

    val settings = parseSettings(actionSettings.generic)

    /* List of files to search in. For now only support the current file */
    var mFileNames: MutableList<VirtualFile> = mutableListOf()
    var mMatches: MutableList<VirtualFile> = mutableListOf()
    // Guards mutations of mMatches. The search coroutine reduces mMatches eagerly
    // (see findMatchesInFile) on its own thread, while the popup may call
    // mMatches.clear()/addAll() on another thread when the query is reset. Both
    // must be serialised so the ArrayList is not corrupted.
    private val mMatchesLock = Any()
    // Guards mFileNames. The walk job writes it (once per walk) on mSearchDispatcher;
    // the popup's pooled thread reads it via getSortedResult. Serialise so the
    // read sees a consistent list and the reference itself isn't torn.
    private val mFileNamesLock = Any()
    var mPopup: SearchPopupInstance<StringMatchInstanceItem>? = null
    var mEvent: AnActionEvent? = null
    var mHistory: History? = null

    /* Keep track of the latest search query */
    var mSearchQuery: String = ""

    /* Keep track of the latest selected text used as search query */
    var mSelectedText: String? = ""
    var mProject: Project? = null

    // Bounded LRU content cache: avoids re-reading every file from disk on
    // every keystroke. Sized in MB via the user setting.
    private val mContentCache = ContentCache(globalSettings.string.contentCacheMaxMB.toLong() * 1024L * 1024L)

    val cpuDispatcher = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
                                                    ).asCoroutineDispatcher()

    // Single-threaded dispatcher used to run the per-query search. A dedicated thread
    // (not shared with the IDEA write-lock pool) is required so the popup's pooled
    // thread can safely wait for the current query's result without deadlocking
    // the EDT's write-lock acquisition, which also needs free pooled threads.
    private val mSearchDispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, "GrepInFiles-search").apply { isDaemon = true }
    }.asCoroutineDispatcher()

    private val mSearchScope = CoroutineScope(SupervisorJob() + mSearchDispatcher)
    private var mSearchJob: Deferred<List<StringMatchInstanceItem>>? = null
    // Background walk that populates mFileNames/mMatches at popup-open time.
    // Runs on mSearchDispatcher (same single-thread as the per-query search);
    // the search job queues behind it, so a slow walk delays the first search
    // but doesn't lose it.
    private var mFileNamesWalkJob: Deferred<Unit>? = null
    private var mLastResult: List<StringMatchInstanceItem> = emptyList()

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val curFile = getCurrentFile(e) ?: return
        mEvent = e
        mFileNames.clear()
        mProject = project

        // Cancel any in-flight search and any in-flight walk from a previous
        // popup invocation so they can't post a stale result back to the new
        // popup.
        mSearchJob?.cancel()
        mSearchJob = null
        mFileNamesWalkJob?.cancel()

        // Sync cache size to current setting (user may have changed it between
        // popup opens).
        mContentCache.setMaxBytes(globalSettings.string.contentCacheMaxMB.toLong() * 1024L * 1024L)

        val previousSearchQuery = mHistory?.query ?: ""
        mPopup = SearchPopupInstance(
            HighlightedStringCellRenderer(project, globalSettings, globalSettings.showFilenameForGrepInFiles),
            ::getSortedResult,
            ::moveToLocation,
            ::getFileFromItem,
            globalSettings,
            project,
            settings.extensionList,
            globalSettings.string,
            "Live grep",
            "",
            previousSearchQuery)

        // Cancel the walk if the popup is disposed (user closed it before the
        // walk finished, popup was cancelled by re-invocation, etc.). The walk
        // is cancelable because VfsUtilCore.visitChildrenRecursively checks
        // coroutine cancellation between nodes via the underlying VFS API.
        // mPopup is a SearchPopupInstance; the underlying JBPopup (which is
        // Disposable) is mPopup!!.mPopup.
        val jbPopup = mPopup!!.mPopup
        if (jbPopup != null) {
            Disposer.register(jbPopup) { mFileNamesWalkJob?.cancel() }
        }

        // Get search history and select from history or selected text (or empty)
        var initialQuery = ""
        val time = if (mHistory == null) 0 else mHistory!!.timeMs
        val historyWithinTimeout =
                (System.currentTimeMillis() - time) < (globalSettings.grepRememberPreviousQuerySeconds * 1000)
        val selectedText = getSelectedText(e)
        if (selectedText != null &&
            (mSelectedText != selectedText && mSearchQuery != selectedText) // If selected text is still the same but query has changed
        ) {
            initialQuery = selectedText
            mSelectedText = selectedText
        } else if (historyWithinTimeout && mHistory!!.query.isNotEmpty()) {
            initialQuery = mHistory!!.query
        }

        mPopup!!.showPopupInstance(initialQuery)

        // Now that the popup is up, do the (potentially slow) VFS walk on
        // mSearchDispatcher. The walk populates mFileNames/mMatches and then
        // refreshes the popup. If the user types before the walk finishes,
        // getSortedResult will wait up to 5s for this job.
        val myWalkJob: Deferred<Unit> = mSearchScope.async {
            try {
                val files = utils.getAllFilesInLocation(
                    curFile,
                    project,
                    settings.path,
                    globalSettings,
                    settings.extensionList,
                    settings.onlyVcsTracked,
                    ::isTextOrCodeFile)
                synchronized(mFileNamesLock) {
                    mFileNames = files
                }
                synchronized(mMatchesLock) {
                    mMatches.clear()
                    mMatches.addAll(files)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("ERROR: Exception during file walk: ${e.message}")
            }
        }
        mFileNamesWalkJob = myWalkJob

        val app = com.intellij.openapi.application.ApplicationManager.getApplication()
        myWalkJob.invokeOnCompletion {
            if (myWalkJob.isCancelled) return@invokeOnCompletion
            app.invokeLater {
                val popup = mPopup
                if (mFileNamesWalkJob === myWalkJob && popup != null && popup.isOpen()) {
                    popup.updateListedItems()
                }
            }
        }
    }

    fun getSortedResult(query: String): List<StringMatchInstanceItem> {
        val trimmedQuery = query.trim().lowercase()

        mHistory = History(trimmedQuery, timeMs = System.currentTimeMillis())
        if (trimmedQuery.isEmpty()) {
            mSearchJob?.cancel()
            mSearchJob = null
            synchronized(mFileNamesLock) {
                synchronized(mMatchesLock) {
                    mMatches.clear()
                    mMatches.addAll(mFileNames)
                }
            }
            mSearchQuery = trimmedQuery
            mLastResult = emptyList()
            return emptyList()
        }

        // For non-empty queries, wait for the file walk to finish (5s soft cap)
        // so the search runs over the full file list. The empty-query path above
        // deliberately does NOT wait — the popup's first refresh fires with an
        // empty query while the walk may still be in flight, and waiting there
        // would freeze the popup's list (showing nothing for up to 5s) even
        // though the popup itself is already visible. The walk's
        // invokeOnCompletion handler refreshes the popup when it finishes.
        val walkJob = mFileNamesWalkJob
        if (walkJob != null) {
            runBlocking { withTimeoutOrNull(5000) { walkJob.await() } }
        }

        // Short-circuit: if the popup re-invokes us with the same query (e.g. when our
        // invokeOnCompletion handler triggers a list refresh), don't relaunch a new
        // search and don't queue another completion handler — that would loop forever.
        // Return the last completed result, which is what the popup should display.
        if (trimmedQuery == mSearchQuery) {
            return mLastResult
        }

        // Reset the file list to all files only when the new query is not a
        // strict extension of the previous one. If the user only appended
        // characters to the previous query, files already known to have no
        // match can be skipped (mMatches still holds the reduced set from
        // the previous search). When the user removes or changes characters
        // (i.e. the new query does not start with the old one), every file
        // has to be re-examined.
        if (mSearchQuery.isEmpty() || !trimmedQuery.startsWith(mSearchQuery)) {
            // New query needs to search all files
            synchronized(mFileNamesLock) {
                synchronized(mMatchesLock) {
                    mMatches.clear()
                    mMatches.addAll(mFileNames)
                }
            }
        }
        mSearchQuery = trimmedQuery

        // Cancel any in-flight search from a previous keystroke and start a fresh one.
        // The search runs on a dedicated single-threaded dispatcher (mSearchDispatcher)
        // that is NOT part of IDEA's pooled-threads (which the platform needs to grant
        // write permits for the EDT). This callback MUST be non-blocking: the popup
        // invokes us on its own pooled thread, and blocking it would starve the EDT's
        // write-lock acquisition and freeze the IDE. We return the previous completed
        // result immediately, then trigger a popup refresh when the new result is ready.
        mSearchJob?.cancel()
        val myJob = mSearchScope.async {
            try {
                // Snapshot mMatches under the lock so we get a consistent view
                // of the (possibly already partially reduced) file list.
                val filesToSearch = synchronized(mMatchesLock) { mMatches.toList() }
                val r = grepForString(trimmedQuery, filesToSearch)
                mLastResult = r
                r
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("ERROR: Exception during grepForString: ${e.message}")
                emptyList()
            }
        }
        mSearchJob = myJob

        // Notify the popup on the EDT so the list model updates with the fresh result.
        // Only refresh if this is still the latest search, otherwise a newer keystroke's
        // search is in flight and we don't want to overwrite its pending UI update.
        // The popup re-invokes us from updateListedItems with the same query string,
        // which now short-circuits to mLastResult above, breaking the loop. Also skip
        // the refresh if the popup has been disposed in the meantime.
        val app = com.intellij.openapi.application.ApplicationManager.getApplication()
        myJob.invokeOnCompletion {
            if (myJob.isCancelled) return@invokeOnCompletion
            app.invokeLater {
                val popup = mPopup
                if (mSearchJob === myJob && popup != null && popup.isOpen()) {
                    popup.updateListedItems()
                }
            }
        }

        return mLastResult
    }

    private suspend fun grepForString(query: String, filesToSearch: List<VirtualFile>): List<StringMatchInstanceItem> {
        val result = Collections.synchronizedList(mutableListOf<StringMatchInstanceItem>())
        val nofFilesToSearch = filesToSearch.size
        val cap = globalSettings.string.numberOfFilesInSearchView

        val timeTaken = measureTimeMillis {
            if (globalSettings.string.searchMultiThreaded) {
                // Cooperative cancellation: when one chunk signals that the
                // result cap is reached, sibling chunks stop at the next
                // isActive check instead of finishing their full file scan.
                // The cap-bound work is O(chunk_size) per chunk (currently
                // 20), which is small compared to the O(matches_capped) work
                // the chunk that hit the cap has already done.
                coroutineScope {
                    filesToSearch.indices.chunked(20).map { chunk ->
                        async(cpuDispatcher) {
                            for (index in chunk) {
                                if (!currentCoroutineContext().isActive) return@async
                                if (index >= filesToSearch.size) break
                                if (!findMatchesInFile(
                                        filesToSearch[index], query, result, cap)) {
                                    return@async
                                }
                            }
                        }
                    }.awaitAll()
                }
            } else {
                for (index in filesToSearch.indices) {
                    if (!currentCoroutineContext().isActive) break
                    if (!findMatchesInFile(filesToSearch[index], query, result, cap)) {
                        break
                    }
                }
            }
            if (!currentCoroutineContext().isActive) return result

            // We do this here to prevent doing this on the UI thread even though this is horrible and ugly
            result.forEach {
                if (globalSettings.string.showFileIcon && it.icon == null) it.icon = it.vf.fileType.icon
            }
        }

        if (globalSettings.common.enableDebugOptions && nofFilesToSearch > 0) {
            println("GrepInFiles: ${result.size} in $timeTaken ms. Nof files to search: ${nofFilesToSearch} (${(timeTaken * 1000) / nofFilesToSearch} us/file), total nof files: ${mFileNames.size}")
        }

        return result
    }

    fun isTextOrCodeFile(file: VirtualFile): Boolean {
        val fileTypeManager = FileTypeManager.getInstance()
        val ext = file.extension
        // Fast path: hashmap lookup by extension. For extensionless files
        // (Dockerfile, Makefile, .env, LICENSE, …) fall back to the slower
        // content-aware detector so we don't drop them.
        val fileType = if (!ext.isNullOrEmpty()) {
            fileTypeManager.getFileTypeByExtension(ext)
        } else {
            fileTypeManager.getFileTypeByFile(file)
        }
        return fileType is PlainTextFileType || fileType.isBinary.not()
    }

    /**
     * Search a single file for [query] and append up to [cap] matches to [matches].
     * Returns false once the cap is reached so the caller can stop scheduling
     * more files. Stale / unreadable files are dropped from mMatches so the
     * next keystroke doesn't repeat the work.
     */
    fun findMatchesInFile(
        vf: VirtualFile,
        query: String,
        matches: MutableList<StringMatchInstanceItem>,
        cap: Int): Boolean {
        // A stale VFS entry (file deleted / moved / not yet indexed during
        // project open) can throw at any of the calls below. Wrap the whole
        // body so one bad file is skipped and the rest of the search continues.
        try {
            if (vf.name.endsWith("min.js")) {
                return true
            }
            // vf.length can throw on a stale entry; treat as "skip" (return
            // true so the search loop continues) rather than aborting.
            val length = try { vf.length } catch (e: Throwable) {
                if (globalSettings.common.enableDebugOptions) {
                    println("GrepInFiles: skipping ${vf.name}: ${e.message}")
                }
                return true
            }
            val sizeCap = globalSettings.string.maxFileSizeMB.toLong() * 1024L * 1024L
            if (sizeCap > 0L && length > sizeCap) return true

            // Read text via the content cache when possible. The cache stores
            // a precomputed newline-offset array so the per-match line
            // computation is a binary search instead of a runReadAction call.
            val cachedText = mContentCache.get(vf)
            val fromCache = cachedText != null
            val contents: String = if (fromCache) {
                cachedText!!
            } else {
                val read = readFileContents(vf)
                if (read.isEmpty()) {
                    // File is unreadable or invalid; if we still have it in
                    // mMatches, drop it so the next keystroke doesn't try again.
                    synchronized(mMatchesLock) { mMatches.remove(vf) }
                    return true
                }
                mContentCache.put(vf, read)
                read
            }

            val document: Document? = if (globalSettings.useFullEditorHighlighting) {
                try {
                    ReadAction.compute<Document?, Exception> { FileDocumentManager.getInstance().getDocument(vf) }
                } catch (e: Throwable) {
                    if (globalSettings.common.enableDebugOptions) {
                        println("GrepInFiles: no document for ${vf.name}: ${e.message}")
                    }
                    null
                }
            } else null

            val caseSensitive = globalSettings.common.searchCaseSensitivity
            val skipLines = globalSettings.minNofLinesBetweenGrepResults

            // Flat character-stream scan, matching the original semantics:
            //  - indexOf from the current position,
            //  - record the match, then advance past `skipLines` newlines,
            //  - resume searching from the start of the next line.
            // We do this on the cached String (no per-match I/O), and the
            // per-match line number is either a binary search on the cache
            // (cheap) or, for cache misses, a single runReadAction (same as
            // before but only one call per file because the file was just read).
            var searchFrom = 0
            var firstMatch = -1
            while (searchFrom <= contents.length) {
                val pos = contents.indexOf(query, searchFrom, !caseSensitive)
                if (pos < 0) break
                if (firstMatch < 0) firstMatch = pos
                if (matches.size >= cap) return false
                val lineNr = lineNumberForMatch(vf, pos, fromCache)
                val (lineText, displayStart, displayEnd) = extractLine(contents, pos)
                matches.add(StringMatchInstanceItem(
                    vf, displayStart, displayEnd, lineNr, lineText, document, ""))

                // Skip `skipLines` newlines before searching again.
                var advance = pos + query.length
                var skipped = 0
                while (skipped < skipLines) {
                    val nl = contents.indexOf('\n', advance)
                    if (nl < 0) {
                        // No more newlines in the file: no more matches.
                        return true
                    }
                    advance = nl + 1
                    skipped++
                }
                searchFrom = advance
            }

            // No-match reduction for the next keystroke (only when the file
            // was read from disk on this call — if it was a cache hit, the
            // previous caller already had the chance to reduce).
            if (firstMatch < 0 && !fromCache) {
                synchronized(mMatchesLock) { mMatches.remove(vf) }
            }
        } catch (e: Throwable) {
            // Last-resort safety net: any platform call that escaped the
            // narrower try/catches above (e.g. text comparison on a
            // half-loaded document) shouldn't abort the parallel search.
            if (globalSettings.common.enableDebugOptions) {
                println("GrepInFiles: skipping ${vf.name} due to: ${e.message}")
            }
            synchronized(mMatchesLock) { mMatches.remove(vf) }
        }
        return true
    }

    /**
     * Line-number lookup for a match. If the file was loaded from the
     * content cache, the line number is a binary search over the cached
     * newline-offset array. Otherwise we fall back to the existing
     * runReadAction-based path (only happens for the very first read of a
     * file in this popup's lifetime).
     */
    private fun lineNumberForMatch(
        vf: VirtualFile,
        offset: Int,
        fromCache: Boolean): Int {
        if (fromCache) {
            return mContentCache.lineNumberAt(vf, offset)
        }
        // Cold path: no cache entry. Use the original read-action lookup.
        return utils.getLineNumberFromVirtualFile(
            vf, offset, globalSettings.common.enableDebugOptions) ?: 1
    }

    /**
     * Returns (displayText, displayStart, displayEnd) for the line containing
     * [matchOffset] in [contents]. The display text has leading and trailing
     * whitespace stripped (mirrors the original findTextBetweenNewlines).
     */
    private fun extractLine(
        contents: String,
        matchOffset: Int): Triple<String, Int, Int> {
        val beforeNl = contents.lastIndexOf('\n', matchOffset - 1).let { if (it < 0) 0 else it + 1 }
        val afterNl = contents.indexOf('\n', matchOffset).let { if (it < 0) contents.length else it }
        var displayStart = beforeNl
        while (displayStart < afterNl && contents[displayStart] == ' ') displayStart++
        return Triple(
            contents.substring(displayStart, afterNl).trim(),
            displayStart,
            afterNl)
    }

    fun moveToLocation(item: StringMatchInstanceItem, location: OpenLocation) {
        val event = mEvent ?: return
        if (item.vf != getCurrentFile(event)) {
            openFileWithLocation(item.vf, location, mProject!!)
        }

        val editor = FileEditorManager.getInstance(mProject!!).selectedTextEditor
        editor?.caretModel?.moveToOffset(item.start)
        editor?.scrollingModel?.scrollToCaret(ScrollType.CENTER_UP)

        mEvent = null
    }

    fun getFileFromItem(item: StringMatchInstanceItem): FileLocation {
        return FileLocation(item.vf, item.start)
    }

    private fun getCurrentFile(e: AnActionEvent): VirtualFile? {
        val editor = e.getData(CommonDataKeys.EDITOR)
        return editor?.virtualFile
    }

    fun getSelectedText(e: AnActionEvent): String? {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
        return editor.selectionModel.selectedText
    }

    private fun readFileContents(virtualFile: VirtualFile): String {
        // The exists()/isValid() check is necessary but not sufficient during
        // project open / indexing: the VFS can mark a file valid while the
        // underlying file has been deleted or is locked. The actual stream
        // open + read can still throw IOException, IllegalArgumentException,
        // or AssertionError. Catch Throwable so the caller sees an empty
        // string instead of aborting the whole search.
        return if (virtualFile.exists() && virtualFile.isValid) {
            try {
                virtualFile.inputStream.bufferedReader().use { it.readText() }
            } catch (e: Throwable) {
                if (globalSettings.common.enableDebugOptions) {
                    println("Error reading file ${virtualFile.name}: ${e.message}")
                }
                ""
            }
        } else {
            if (globalSettings.common.enableDebugOptions) {
                println("Error reading file ${virtualFile.name}: not valid or does not exist")
            }
            ""
        }
    }

    companion object {
        fun parseSettings(actionSettings: List<String>): Settings {
            val settings = Settings(
                path = actionSettings[0],
                extensionList = utils.extractExtensions(actionSettings[1]),
                onlyVcsTracked = actionSettings.getOrElse(2) { "true" }.toBoolean())
            return settings
        }

        fun register(settings: utils.ActionSettings, globalSettings: GlobalSettings.SettingsState) {
            val action = GrepInFiles(settings, globalSettings)
            utils.registerAction(settings.name, settings.shortcut, action)
        }
    }
}
