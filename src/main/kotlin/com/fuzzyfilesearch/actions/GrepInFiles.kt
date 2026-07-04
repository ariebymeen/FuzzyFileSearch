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
import com.jetbrains.rd.framework.base.deepClonePolymorphic
import fleet.rpc.remoteApiDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.*
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
            } catch (e: kotlinx.coroutines.CancellationException) {
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
            } catch (e: kotlinx.coroutines.CancellationException) {
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
        // TODO: Support wildcards
        val result = Collections.synchronizedList(mutableListOf<StringMatchInstanceItem>())
        val nofFilesToSearch = filesToSearch.size

        val timeTaken = measureTimeMillis {
            // Loop over all files and find regex matches. Store result into class variable to use in search
            if (globalSettings.string.searchMultiThreaded) {
                coroutineScope {
                    filesToSearch.indices.chunked(20).map { chunk ->
                        async(cpuDispatcher) {
                            for (index in chunk) {
                                if (!currentCoroutineContext().isActive) return@async
                                if (index >= filesToSearch.size) break
                                if (!findMatchesInFile(filesToSearch[index], query, result)) {
                                    return@async
                                }
                            }
                        }
                    }.awaitAll()
                }
            } else {
                for (index in filesToSearch.indices) {
                    if (!currentCoroutineContext().isActive) break
                    if (!findMatchesInFile(filesToSearch[index], query, result)) {
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

    fun findMatchesInFile(
        vf: VirtualFile,
        query: String,
        matches: MutableList<StringMatchInstanceItem>): Boolean {
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
            if (length > 200000) return true

            val contents = readFileContents(vf)
            if (contents.isEmpty()) {
                // File is unreadable or invalid; if we still have it in
                // mMatches, drop it so the next keystroke doesn't try again.
                synchronized(mMatchesLock) { mMatches.remove(vf) }
                return true
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
            var match = contents.indexOf(query, 0, !globalSettings.common.searchCaseSensitivity)
            if (match < 0) {
                // No match in this file: remove it from mMatches immediately so that
                // the reduction is visible to the next keystroke even if this search
                // is cancelled before it would normally apply the deferred removeAll.
                // This is the core of the "skip files known to have no match when the
                // query grows" optimisation: it must take effect synchronously, not at
                // the end of an asynchronous search.
                synchronized(mMatchesLock) {
                    mMatches.remove(vf)
                }
            }
            while (match >= 0 && matches.size < globalSettings.string.numberOfFilesInSearchView) {
                val text = findTextBetweenNewlines(contents, match)
                val lineNr = utils.getLineNumberFromVirtualFile(vf, text.second, globalSettings.common.enableDebugOptions) ?: 0
                matches.add(StringMatchInstanceItem(vf, text.second, text.third, lineNr, text.first.trim(), document, ""))
                match = text.third

                // Skip a number of lines, these are not searched through
                for (i in 0 until globalSettings.minNofLinesBetweenGrepResults) {
                    match = getStartOfNextNewline(contents, match + 1)
                }

                match = contents.indexOf(query, match, !globalSettings.common.searchCaseSensitivity)

                if (matches.size >= globalSettings.string.numberOfFilesInSearchView) {
                    return false
                }
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

    private fun getStartOfNextNewline(text: String, index: Int): Int {
        return text.indexOf('\n', index).takeIf { it != -1 } ?: text.length
    }

    private fun findTextBetweenNewlines(text: String, index: Int): Triple<String, Int, Int> {
        if (index < 0 || index >= text.length) {
            throw IndexOutOfBoundsException("Index is out of the string's range.")
        }

        val beforeIndex = text.lastIndexOf('\n', index - 1).takeIf { it != -1 } ?: 0
        var startIndex = beforeIndex + 1
        while (startIndex < text.length && text[startIndex] == ' ') {
            startIndex += 1
        }
        val afterIndex = text.indexOf('\n', index).takeIf { it != -1 } ?: text.length

        return Triple(text.substring(startIndex, afterIndex).trim(), startIndex, afterIndex)
    }

    private fun getPrecedingContext(text: String, lineStart: Int, maxChars: Int = 256): String {
        if (lineStart <= 0) return ""
        val from = maxOf(0, lineStart - maxChars)
        val slice = text.substring(from, lineStart)
        val lastNewline = slice.lastIndexOf('\n')
        return if (lastNewline >= 0) slice.substring(lastNewline + 1) else slice
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