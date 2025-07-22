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
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.regex.PatternSyntaxException
import kotlin.math.min
import kotlin.system.measureTimeMillis

class RegexMatchInFiles(val action: Array<String>,
                        val settings: GlobalSettings.SettingsState) : AnAction(getActionName(action)) {

    /* List of files to search in. For now only support the current file */
    var mFileNames: MutableList<VirtualFile> = mutableListOf()
    var mPopup: SearchPopupInstance<StringMatchInstanceItem>? = null
    var mEvent: AnActionEvent? = null

    /* List with instance items that matched the regex */
    var mSearchItems = mutableListOf<StringMatchInstanceItem>()
    var mRegex: Regex? = null
    /* The same list but only the matching string. Is used in the search action to search */
    var mSearchItemStrings = emptyList<String>()
    var mProject: Project? = null
    var fzfSearchAction: FzfSearchAction? = null

    init {
        try {
            mRegex = Regex(getActionRegex(action))
        } catch (e: PatternSyntaxException) {
            showErrorNotification("Invalid regex", "Regex ${getActionRegex(action)} is invalid: ${e.message}")
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (mRegex == null) {
            showErrorNotification("Invalid regex", "Regex ${getActionRegex(action)} is invalid")
            return
        }
        val project = e.project?: return
        val curFile = getCurrentFile(e)?: return
        mEvent = e
        mFileNames.clear()
        mSearchItems.clear()

        mProject = project
        mFileNames = getAllFilesInLocation(curFile, project, getActionPath(action), settings, getActionExtension(action))

        val timeTaken = measureTimeMillis {
            // Loop over all files and find regex matches. Store result into class variable to use in search
            mFileNames.forEach{ vf ->
                val matches = mRegex!!.findAll(readFileContents(vf)).toList()
                mSearchItems.addAll(matches.map { match ->
                    // Remove newlines and indenting for to make the view single line
                    // TODO: This may not be the most efficient way to do any of this
                    var oldText = match.value.replace('\n', ' ')
                    var newText = oldText.replace("  ", " ")
                    while (newText != oldText) {
                        oldText = newText
                        newText = newText.replace("    ", " ")
                                         .replace("   " , " ")
                                         .replace("  "  , " ")
                    }
                    val line_nr = getLineNumberFromVirtualFile(vf, match.range.first)?: 0
                    StringMatchInstanceItem(vf, match.range.first, match.range.last, line_nr, newText)
                })

            }
            mSearchItemStrings = mSearchItems.map { item -> item.text }
        }

        // TODO: Remove debug prints
        println("Elapsed time: $timeTaken ms")
        println("GrepInFiles: ${mSearchItems.size}. Nof files to search: ${mFileNames.size}, regex: ${getActionRegex(action)}, action: ${action.joinToString(",")}")

        val showFileName = settings.showFilenameForRegexMatch == ShowFilenamePolicy.ALWAYS || (settings.showFilenameForRegexMatch == ShowFilenamePolicy.WHEN_SEARCHING_MULTIPLE_FILES && mFileNames.size > 1)
        mPopup = SearchPopupInstance(HighlightedStringCellRenderer(project, settings, showFileName), ::getSortedResult, ::moveToLocation, ::getFileFromItem,
                                                                            settings, project, getActionExtension(action),
                                                                            settings.string,
                                                                            "Regex search")
        mPopup!!.showPopupInstance()
        fzfSearchAction = FzfSearchAction(mSearchItemStrings, settings.common.searchCaseSensitivity)
    }

    fun getSortedResult(query: String) : List<StringMatchInstanceItem> {
        if (query.isNotEmpty()) {
            val filtered = fzfSearchAction!!.search(query)
            val visibleList = filtered.subList(0, min(filtered.size, settings.string.numberOfFilesInSearchView))
            val visibleItems = visibleList
                .map { file -> mSearchItemStrings.indexOfFirst{ name  -> name == file } }
                .map { index ->
                    if (index >= 0) {
                        mSearchItems[index]
                    } else {
                        println("Error, unexpected index $index. Filtered files size: ${filtered.size}, file size: ${mSearchItems.size}")
                        showErrorNotification("Something went wrong searching", "Error searching mFiles: $visibleList, invalidating caches now")
                        mSearchItems[0]
                    }
                }
            // We do this here to prevent doing this on the UI thread even though this is horrible and ugly
            visibleItems.forEach {
                if (settings.string.showFileIcon && it.icon == null) it.icon = it.vf.fileType.icon
            }
            return visibleItems
        }
        else {
            return mSearchItems.subList(0, min(mSearchItems.size, settings.string.numberOfFilesInSearchView))
        }
    }

    fun moveToLocation(item: StringMatchInstanceItem, location: OpenLocation) {
        val event = mEvent?: return
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

    private fun getCurrentFile(e: AnActionEvent) : VirtualFile? {
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
        fun getActionName(actionSettings: Array<String>) : String {
            return actionSettings[0]
        }
        fun getActionPath(actionSettings: Array<String>) : String {
            return actionSettings[1]
        }
        fun getActionRegex(actionSettings: Array<String>) : String {
            return actionSettings[2]
//            return """fun\s++\w+\s*\([\s\S]*?\)\s*(?::\s*[\w<>.?]+)?\s*\{"""
        }
        fun getActionShortcut(actionSettings: Array<String>) : String {
            return actionSettings[3]
        }
        fun getActionExtension(actionSettings: Array<String>) : List<String> {
            if (actionSettings.size <= 4) return emptyList()
            return extractExtensions(actionSettings[4])
        }
    }

}