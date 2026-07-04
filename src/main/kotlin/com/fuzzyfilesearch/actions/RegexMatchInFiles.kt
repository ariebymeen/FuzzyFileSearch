package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.renderers.HighlightedStringCellRenderer
import com.fuzzyfilesearch.renderers.StringMatchInstanceItem
import com.fuzzyfilesearch.searchbox.*
import com.fuzzyfilesearch.settings.GlobalSettings
import com.fuzzyfilesearch.settings.ShowFilenamePolicy
import com.fuzzyfilesearch.showErrorNotification
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.math.min
import kotlin.system.measureTimeMillis

class RegexMatchInFiles(
    val actionSettings: utils.ActionSettings,
    val globalSettings: GlobalSettings.SettingsState) : AnAction(actionSettings.name) {

    data class Settings(
        val regex: Regex,
        val path: String,
        val extensionList: List<String>,
        var onlyVcsTracked: Boolean)

    val settings = parseSettings(actionSettings.generic)

    /* List of files to search in. For now only support the current file */
    var mFileNames: MutableList<VirtualFile> = mutableListOf()
    var mPopup: SearchPopupInstance<StringMatchInstanceItem>? = null
    var mEvent: AnActionEvent? = null
    // Allow restoring search query by pressing array up before typing
    var mPreviousSearchQuery: String = ""

    /* List with instance items that matched the regex */
    var mSearchItems = mutableListOf<StringMatchInstanceItem>()

    /* The same list but only the matching string. Is used in the search action to search */
    var mSearchItemStrings = emptyList<String>()
    var mProject: Project? = null

    override fun actionPerformed(e: AnActionEvent) {
        if (settings.regex.pattern.isEmpty()) {
            showErrorNotification("Invalid regex", "Regex for ${actionSettings.name} is invalid")
            return
        }

        val project = e.project ?: return
        val curFile = getCurrentFile(e) ?: return

        mEvent = e
        mFileNames.clear()
        mSearchItems.clear()

        mProject = project
        val files = utils.getAllFilesInLocation(
            curFile,
            project,
            settings.path,
            globalSettings,
            settings.extensionList,
            settings.onlyVcsTracked)
        mFileNames.clear()
        mFileNames.addAll(files)

        val timeTaken = measureTimeMillis {
            // Loop over all files and find regex matches. Store result into class variable to use in search
            mFileNames.forEach { vf ->
                val fileText = readFileContents(vf)
                val document: Document? = if (globalSettings.useFullEditorHighlighting) {
                    ReadAction.compute<Document?, Exception> { FileDocumentManager.getInstance().getDocument(vf) }
                } else null
                val matches = settings.regex.findAll(fileText).toList()
                mSearchItems.addAll(matches.map { match ->
                    // Remove newlines and indenting for to make the view single line
                    // TODO: This may not be the most efficient way to do any of this
                    var oldText = match.value.replace('\n', ' ')
                    var newText = oldText.replace("  ", " ")
                    while (newText != oldText) {
                        oldText = newText
                        newText = newText.replace("    ", " ")
                            .replace("   ", " ")
                            .replace("  ", " ")
                    }
                    val preceding = getPrecedingContext(fileText, match.range.first)
                    val lineNr = utils.getLineNumberFromVirtualFile(
                        vf,
                        match.range.first,
                        globalSettings.common.enableDebugOptions) ?: 0
                    StringMatchInstanceItem(vf, match.range.first, match.range.last, lineNr, newText, document,preceding)
                })

            }
            mSearchItemStrings = mSearchItems.map { item -> item.text }
        }

        if (globalSettings.common.enableDebugOptions) {
            println("Elapsed time: $timeTaken ms")
            println(
                "RegexMatchInFiles: ${mSearchItems.size}. Nof files to search: ${mFileNames.size}, regex: ${settings.regex.pattern}, " +
                "action: ${actionSettings.name}")
        }

        val showFileName = globalSettings.showFilenameForRegexMatch == ShowFilenamePolicy.ALWAYS ||
                           (globalSettings.showFilenameForRegexMatch == ShowFilenamePolicy.WHEN_SEARCHING_MULTIPLE_FILES && mFileNames.size > 1)
        mPopup = SearchPopupInstance(
            HighlightedStringCellRenderer(project, globalSettings, showFileName),
            ::getSortedResult,
            ::moveToLocation,
            ::getFileFromItem,
            globalSettings,
            project,
            settings.extensionList,
            globalSettings.string,
            "Regex search",
            "",
            mPreviousSearchQuery)
        mPopup!!.showPopupInstance("", mSearchItems.size)
    }

    private fun searchFzf(strings: List<String>, query: String): Pair<List<String>, IntArray> {
        var searchFunc = ::FuzzyMatchV2
        if (strings.size > 200) {
            searchFunc = ::FuzzyMatchV1
        }

        val queryNorm = if (globalSettings.common.searchCaseSensitivity) query else query.lowercase()
        val scores = IntArray(strings.size)
        for (index in strings.indices) {
            scores[index] = searchFunc(globalSettings.common.searchCaseSensitivity, strings[index], queryNorm).score
        }

        // Single-pass collect: build a parallel (index, score) list of
        // non-zero entries, sort by score desc, and return both the sorted
        // paths and their original indices. The original-index list lets
        // the caller map visible results back to mSearchItems in O(K) —
        // replacing the previous O(K * M) indexOfFirst scan.
        val n = strings.size
        var nonZero = 0
        for (i in 0 until n) {
            if (scores[i] != 0) nonZero++
        }
        if (nonZero == 0) return Pair(emptyList(), IntArray(0))
        val indices = IntArray(nonZero)
        val filteredScores = IntArray(nonZero)
        var k = 0
        for (i in 0 until n) {
            if (scores[i] != 0) {
                indices[k] = i
                filteredScores[k] = scores[i]
                k++
            }
        }
        // Sort indices in-place by their score descending. Box into IntArray
        // of pairs via parallel arrays so we can use a Comparator.
        val boxed = arrayOfNulls<Int>(nonZero)
        for (i in 0 until nonZero) boxed[i] = indices[i]
        java.util.Arrays.sort(boxed, 0, nonZero,
            java.util.Comparator { a, b -> filteredScores[b!!] - filteredScores[a!!] })
        for (i in 0 until nonZero) indices[i] = boxed[i]!!

        val sortedStrings = ArrayList<String>(nonZero)
        for (i in 0 until nonZero) sortedStrings.add(strings[indices[i]])
        return Pair(sortedStrings, indices)
    }

    fun getSortedResult(query: String): List<StringMatchInstanceItem> {
        if (query.isNotEmpty()) {
            val (_, sortedIndices) = searchFzf(mSearchItemStrings, query)
            val visibleItems = mapVisibleItems(
                sortedIndices,
                globalSettings.string.numberOfFilesInSearchView,
                mSearchItems,
                mSearchItems[0])
            // We do this here to prevent doing this on the UI thread even though this is horrible and ugly
            visibleItems.forEach {
                if (globalSettings.string.showFileIcon && it.icon == null) it.icon = it.vf.fileType.icon
            }
            return visibleItems
        } else {
            return mSearchItems.subList(0, min(mSearchItems.size, globalSettings.string.numberOfFilesInSearchView))
        }
    }

    fun moveToLocation(item: StringMatchInstanceItem, location: OpenLocation) {
        mPreviousSearchQuery = mPopup?.getQuery() ?: ""
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

    private fun readFileContents(virtualFile: VirtualFile): String {
        // See GrepInFiles.readFileContents: catch Throwable around the
        // stream open + read so a stale VFS entry doesn't abort the search.
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

    private fun getPrecedingContext(text: String, matchStart: Int, maxChars: Int = 256): String {
        if (matchStart <= 0) return ""
        val from = maxOf(0, matchStart - maxChars)
        val slice = text.substring(from, matchStart)
        val lastNewline = slice.lastIndexOf('\n')
        return if (lastNewline >= 0) slice.substring(lastNewline + 1) else slice
    }

    companion object {
        fun parseSettings(actionSettings: List<String>): Settings {
            val settings = Settings(
                regex = utils.parseRegex(actionSettings[0]),
                path = actionSettings[1],
                extensionList = utils.extractExtensions(actionSettings[2]),
                onlyVcsTracked = actionSettings.getOrElse(3) { "true" }.toBoolean())
            return settings
        }

        fun register(settings: utils.ActionSettings, globalSettings: GlobalSettings.SettingsState) {
            val action = RegexMatchInFiles(settings, globalSettings)
            utils.registerAction(settings.name, settings.shortcut, action)
        }
    }

}