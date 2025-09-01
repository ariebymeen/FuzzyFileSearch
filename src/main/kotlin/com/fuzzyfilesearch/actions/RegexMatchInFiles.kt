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
import com.intellij.openapi.editor.ScrollType
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
        mFileNames = utils.getAllFilesInLocation(
            curFile,
            project,
            settings.path,
            globalSettings,
            settings.extensionList,
            settings.onlyVcsTracked)

        val timeTaken = measureTimeMillis {
            // Loop over all files and find regex matches. Store result into class variable to use in search
            mFileNames.forEach { vf ->
                val matches = settings.regex.findAll(readFileContents(vf)).toList()
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
                    val lineNr = utils.getLineNumberFromVirtualFile(
                        vf,
                        match.range.first,
                        globalSettings.common.enableDebugOptions) ?: 0
                    StringMatchInstanceItem(vf, match.range.first, match.range.last, lineNr, newText)
                })

            }
            mSearchItemStrings = mSearchItems.map { item -> item.text }
        }

        if (globalSettings.common.enableDebugOptions) {
            println("Elapsed time: $timeTaken ms")
            println(
                "GrepInFiles: ${mSearchItems.size}. Nof files to search: ${mFileNames.size}, regex: ${settings.regex.pattern}, " +
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
        mPopup!!.showPopupInstance()
    }

    private fun searchFzf(strings: List<String>, query: String): List<String> {
        var searchFunc = ::FuzzyMatchV2
        if (strings.size > 200) {
            searchFunc = ::FuzzyMatchV1
        }

        val queryNorm = if (globalSettings.common.searchCaseSensitivity) query else query.lowercase()
        val scores = Array<Int>(strings.size) { 0 }
        for (index in strings.indices) {
            scores[index] = searchFunc(globalSettings.common.searchCaseSensitivity, strings[index], queryNorm).score
        }

        val filtered = scores.zip(strings).filter { (num, _) -> num != 0 }
        val sorted = filtered.sortedByDescending{ (num, _) -> num }
        val (_, results) = sorted.unzip()
        return results
    }

    fun getSortedResult(query: String): List<StringMatchInstanceItem> {
        if (query.isNotEmpty()) {
//            val filtered = fzfSearchAction!!.search(query)
            val filtered = searchFzf(mSearchItemStrings, query)
            val visibleList = filtered.subList(0, min(filtered.size, globalSettings.string.numberOfFilesInSearchView))
            val visibleItems = visibleList
                .map { file -> mSearchItemStrings.indexOfFirst { name -> name == file } }
                .map { index ->
                    if (index >= 0) {
                        mSearchItems[index]
                    } else {
                        println("Error, unexpected index $index. Filtered files size: ${filtered.size}, file size: ${mSearchItems.size}")
                        showErrorNotification(
                            "Something went wrong searching",
                            "Error searching mFiles: $visibleList, invalidating caches now")
                        mSearchItems[0]
                    }
                }
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
        return if (virtualFile.exists() && virtualFile.isValid) {
            // Read the file content as a string
            virtualFile.inputStream.bufferedReader().use { it.readText() }
        } else {
            println("Error reading file ${virtualFile.name}")
            return ""
        }
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