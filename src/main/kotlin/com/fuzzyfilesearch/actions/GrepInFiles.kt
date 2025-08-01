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
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rd.framework.base.deepClonePolymorphic
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
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

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val curFile = getCurrentFile(e) ?: return
        mEvent = e
        mFileNames.clear()
        mProject = project

        mFileNames = utils.getAllFilesInLocation(
            curFile,
            project,
            settings.path,
            globalSettings,
            settings.extensionList,
            settings.onlyVcsTracked)
        mFileNames = mFileNames.filter { file -> isTextOrCodeFile(file) }.toMutableList()
        mMatches = mFileNames.deepClonePolymorphic()

        mPopup = SearchPopupInstance(
            HighlightedStringCellRenderer(project, globalSettings, globalSettings.showFilenameForGrepInFiles),
            ::getSortedResult,
            ::moveToLocation,
            ::getFileFromItem,
            globalSettings,
            project,
            settings.extensionList,
            globalSettings.string,
            "Live grep")

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
    }

    fun getSortedResult(query: String): List<StringMatchInstanceItem> {
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
        // TODO: Support wildcards
        val result = Collections.synchronizedList(mutableListOf<StringMatchInstanceItem>())
        val itemsToRemove = Collections.synchronizedList(mutableListOf<VirtualFile>())
        val nofFilesToSearch = mMatches.size

        val timeTaken = measureTimeMillis {
            // Loop over all files and find regex matches. Store result into class variable to use in search
            if (globalSettings.string.searchMultiThreaded) {
                runBlocking {
                    mMatches.indices.chunked(10).map { chunk ->
                        async(cpuDispatcher) {
                            for (index in chunk) {
                                if (!findMatchesInFile(mMatches[index], query, result, itemsToRemove)) {
                                    break
                                }
                            }
                        }
                    }.awaitAll()
                }
            } else {
                for (index in mMatches.indices)
                    if (!findMatchesInFile(mMatches[index], query, result, itemsToRemove)) {
                        break
                    }
            }
            mMatches.removeAll(itemsToRemove) // If no matches are found in a file, this file does not need to be searched for the next query (if query grows)

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
        if (!file.isValid || file.isDirectory) return false

        val fileType = FileTypeManager.getInstance().getFileTypeByFile(file)
        return fileType is PlainTextFileType || fileType.isBinary.not()
    }

    fun findMatchesInFile(
        vf: VirtualFile,
        query: String,
        matches: MutableList<StringMatchInstanceItem>,
        nonMatches: MutableList<VirtualFile>): Boolean {
        if (vf.extension.equals("min.js")) return true // Don't search through library files

        val contents = readFileContents(vf)
        var match = contents.indexOf(query, 0, !globalSettings.common.searchCaseSensitivity)
        if (match < 0) {
            nonMatches.add(vf)
        }
        while (match >= 0 && matches.size < globalSettings.string.numberOfFilesInSearchView) {
            val text = findTextBetweenNewlines(contents, match)
            val lineNr =
                    utils.getLineNumberFromVirtualFile(vf, text.second, globalSettings.common.enableDebugOptions) ?: 0
            matches.add(StringMatchInstanceItem(vf, text.second, text.third, lineNr, text.first.trim()))
            match = contents.indexOf(query, text.third, !globalSettings.common.searchCaseSensitivity)

            if (matches.size >= globalSettings.string.numberOfFilesInSearchView) {
                return false
            }
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

        return Triple(text.substring(beforeIndex, afterIndex).trim(), beforeIndex + 1, afterIndex)
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