package com.fuzzyfilesearch.actions

//import com.fuzzyfilesearch.searchbox.*
import com.fuzzyfilesearch.searchbox.*
import com.fuzzyfilesearch.settings.GlobalSettings
import com.fuzzyfilesearch.showErrorNotification
import com.fuzzyfilesearch.showTimedNotification
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import kotlin.io.path.Path
import kotlin.math.min
import kotlin.system.measureTimeMillis

class RegexMatch(val result: MatchResult, val file: VirtualFile)
class GrepInstanceItem(val match: RegexMatch,
                       var panel: VerticallyCenteredTextPane? = null)

class GrepInFiles(val action: Array<String>,
                  val settings: GlobalSettings.SettingsState) : AnAction(getActionName(action)) {

    /* List of files to search in. For now only support the current file */
    var mFileNames: MutableList<VirtualFile> = mutableListOf()
    var mPopup: SearchPopupInstance<GrepInstanceItem>? = null
    var mEvent: AnActionEvent? = null

    /* List with instance items that matched the regex */
    var mSearchItems = mutableListOf<GrepInstanceItem>()
    /* The same list but only the matching string. Is used in the search action to search */
    var mSearchItemStrings = emptyList<String>()
    var mRegex = Regex(getActionRegex(action))
    var mProject: Project? = null
    var fzfSearchAction: FzfSearchAction? = null

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project?: return
        val curFile = getCurrentFile(e)?: return
        mEvent = e
        mFileNames.clear()
        mSearchItems.clear()

        mProject = project

        val searchPath: String
        val location = getActionPath(action)
        if (location.isEmpty() || (location[0] == '.' && location.length == 1)) {
            // Search only current file
            mFileNames.add(curFile)
        } else {
            if (location[0] == '/') { // Search from project root
                searchPath = project.basePath + location
            } else { // Search from current file
                searchPath = curFile.parent.path + "/" + location
            }
            val vfPath = getVirtualFileFromPath(searchPath)?: return
            val changeListManager = if (settings.searchOnlyFilesInVersionControl) ChangeListManager.getInstance(project) else null
            val allFiles = getAllFilesInRoot(vfPath, settings.excludedDirs, emptyList(), changeListManager)
            mFileNames.addAll(allFiles.map { file -> file.vf })
        }

        val timeTaken = measureTimeMillis {
            mFileNames.forEach{ vf ->
                val matches = mRegex.findAll(readFileContents(vf)).toList()
                mSearchItems.addAll(matches.map { match -> GrepInstanceItem(RegexMatch(match, vf)) })
            }
            mSearchItemStrings = mSearchItems.map { item -> item.match.result.value }
        }
        println("Elapsed time: $timeTaken ms")
        println("GrepInFiles: ${mSearchItems.size}. Nof files to search: ${mFileNames.size}, regex: ${getActionRegex(action)}")

        mPopup = SearchPopupInstance(SimpleStringCellRenderer(project, settings), ::getSortedResult, ::moveToLocation, settings, project, emptyList())
        mPopup!!.showPopupInstance()
        fzfSearchAction = FzfSearchAction(mSearchItemStrings, settings.searchCaseSensitivity)
    }

    fun getSortedResult(query: String) : List<GrepInstanceItem> {
        if (query.isNotEmpty()) {
            val filtered = fzfSearchAction!!.search(query)
            val visibleList = filtered.subList(0, min(filtered.size, settings.numberOfFilesInSearchView))
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

            return visibleItems
        }
        else {
            return mSearchItems.subList(0, min(mSearchItems.size, settings.numberOfFilesInSearchView))
        }
    }

    fun moveToLocation(item: GrepInstanceItem, location: OpenLocation) {
        val event = mEvent?: return
        if (item.match.file != getCurrentFile(event)) {
            openFileWithLocation(item.match.file, location, mProject!!)
        }

        val editor = FileEditorManager.getInstance(mProject!!).selectedTextEditor
        editor?.caretModel?.moveToOffset(item.match.result.range.last)
        editor?.scrollingModel?.scrollToCaret(ScrollType.CENTER_UP)

        mEvent = null
    }

    private fun getCurrentFile(e: AnActionEvent) : VirtualFile? {
        val editor = e.getData(CommonDataKeys.EDITOR)
        return editor?.virtualFile
    }

    private fun getVirtualFileFromPath(filePath: String): VirtualFile? {
        val virtualFile = VfsUtil.findFile(Path(filePath), true)
        return virtualFile
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
    }

}