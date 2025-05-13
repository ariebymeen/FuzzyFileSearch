package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.renderers.HighlightedStringCellRenderer
import com.fuzzyfilesearch.renderers.StringMatchInstanceItem
import com.fuzzyfilesearch.searchbox.*
import com.fuzzyfilesearch.settings.GlobalSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import kotlin.io.path.Path
import kotlin.system.measureTimeMillis
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.jetbrains.rd.framework.base.deepClonePolymorphic

class History(val query: String, val timeMs: Long)

class GrepInFiles(val action: Array<String>,
                  val settings: GlobalSettings.SettingsState) : AnAction(getActionName(action)) {

    /* List of files to search in. For now only support the current file */
    var mFileNames: MutableList<VirtualFile> = mutableListOf()
    var mMatches  : MutableList<VirtualFile> = mutableListOf()
    var mPopup: SearchPopupInstance<StringMatchInstanceItem>? = null
    var mEvent: AnActionEvent? = null
    var mHistory: History? = null

    /* Keep track of the latest search query */
    var mSearchQuery: String = ""
    /* The same list but only the matching string. Is used in the search action to search */
    var mSearchItemStrings = emptyList<String>()
    var mProject: Project? = null
    var fzfSearchAction: FzfSearchAction? = null

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project?: return
        val curFile = getCurrentFile(e)?: return
        mEvent = e
        mFileNames.clear()
        mProject = project

        mFileNames = getAllFilesInLocation(curFile, project, getActionPath(action), settings, getActionExtension(action))
        mFileNames = mFileNames.filter { file -> isTextOrCodeFile(file) }.toMutableList()
        mMatches   = mFileNames.deepClonePolymorphic()

        mPopup = SearchPopupInstance(
            HighlightedStringCellRenderer(project, settings, settings.showFilenameForGrepInFiles), ::getSortedResult, ::moveToLocation, ::getFileFromItem,
                                                                            settings, project, getActionExtension(action),
                                                                            settings.string,
                                                                            "Live grep")
        mPopup!!.showPopupInstance()
        fzfSearchAction = FzfSearchAction(mSearchItemStrings, settings.common.searchCaseSensitivity)

        val time = if (mHistory == null) 0 else mHistory!!.timeMs
        val historyWithinTimeout = (System.currentTimeMillis() - time) < (settings.grepRememberPreviousQuerySeconds * 1000)
        if (historyWithinTimeout) {
            mPopup?.mSearchField?.text = mHistory!!.query
        } else {
            val selectedText = getSelectedText(e)
            if (selectedText != null) {
                mPopup?.mSearchField?.text = selectedText
            }
        }
    }


    fun getSortedResult(query: String) : List<StringMatchInstanceItem> {
        val trimmedQuery = query.trim().lowercase()
        mHistory = History(trimmedQuery, timeMs = System.currentTimeMillis())

        if (trimmedQuery.isEmpty()) {
            mMatches = mFileNames.deepClonePolymorphic()
            mSearchQuery = trimmedQuery
            return emptyList()
        }

        if (mSearchQuery.isEmpty() || !trimmedQuery.contains(mSearchQuery)) {
            // New query needs to search all files
            mMatches = mFileNames.deepClonePolymorphic()
        }
        mSearchQuery = trimmedQuery
        return grepForString(trimmedQuery)
    }

    private fun grepForString(query: String): List<StringMatchInstanceItem> {
        // TODO: Allow using ripgrep

        val result = mutableListOf<StringMatchInstanceItem>()
        val nofFilesToSearch = mMatches.size
        val timeTaken = measureTimeMillis {
            // Loop over all files and find regex matches. Store result into class variable to use in search
            val itemsToRemove = mutableListOf<VirtualFile>()
            mMatches.forEach{ vf ->
                val contents = readFileContents(vf)
                var match = contents.indexOf(query, 0, !settings.common.searchCaseSensitivity)
                if (match < 0) {
                    itemsToRemove.add(vf)
                }
                while (match >= 0 && result.size < settings.string.numberOfFilesInSearchView) {
                    val text = findTextBetweenNewlines(contents, match)
                    result.add(StringMatchInstanceItem(vf, text.second, text.third, text.first.trim()))
                    match = contents.indexOf(query, text.third, !settings.common.searchCaseSensitivity)

                    if (result.size >= settings.string.numberOfFilesInSearchView) break
                }
            }
            mMatches.removeAll(itemsToRemove) // If no matches are found in a file, this file does not need to be searched for the next query (if query grows)
        }

        // TODO: Remove debug prints
        println("Elapsed time: $timeTaken ms")
        if (nofFilesToSearch > 0) {
            println("GrepInFiles: ${result.size}. Nof files to search: ${nofFilesToSearch} (${(timeTaken * 1000) / nofFilesToSearch} us/file), total nof files: ${mFileNames.size}")
        }

        return result
    }

    fun isTextOrCodeFile(file: VirtualFile): Boolean {
        if (!file.isValid || file.isDirectory) return false

        val fileType = FileTypeManager.getInstance().getFileTypeByFile(file)
        return fileType is PlainTextFileType || fileType.isBinary.not()
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

    private fun getVirtualFileFromPath(filePath: String): VirtualFile? {
        val virtualFile = VfsUtil.findFile(Path(filePath), true)
        return virtualFile
    }

    fun getSelectedText(e: AnActionEvent): String? {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
        return editor.selectionModel.selectedText
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

    private fun findTextBetweenNewlines(text: String, index: Int): Triple<String, Int, Int> {
        if (index < 0 || index >= text.length) {
            throw IndexOutOfBoundsException("Index is out of the string's range.")
        }

        val beforeIndex = text.lastIndexOf('\n', index - 1).takeIf { it != -1 } ?: 0
        val afterIndex = text.indexOf('\n', index).takeIf { it != -1 } ?: text.length

        return Triple(text.substring(beforeIndex, afterIndex).trim(), beforeIndex, afterIndex)
    }

    companion object {
        fun getActionName(actionSettings: Array<String>) : String {
            return actionSettings[0]
        }
        fun getActionPath(actionSettings: Array<String>) : String {
            return actionSettings[1]
        }
        fun getActionExtension(actionSettings: Array<String>) : List<String> {
            return extractExtensions(actionSettings[2])
        }
        fun getActionShortcut(actionSettings: Array<String>) : String {
            return actionSettings[3]
        }
    }

}