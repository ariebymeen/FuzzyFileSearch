package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.settings.GlobalSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.fuzzyfilesearch.*
import com.fuzzyfilesearch.renderers.FileInstanceItem
import com.fuzzyfilesearch.services.FileWatcher
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VfsUtil
import kotlin.io.path.Path

class SearchFilesWithPatternAction(val action: Array<String>,
                                   val settings: GlobalSettings.SettingsState) : AnAction(getActionName(action))
{
    val regex = Regex(pattern = action[2], options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    val location = action[1]
    val name = getActionName(action)
    var files: List<FileInstanceItem>? = null
    var project: Project? = null
    var searchAction = SearchForFiles(settings)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val searchPath: String
        if (location.isEmpty() || location[0] == '/') { // Search from project root
            searchPath = project.basePath + location
        } else { // Search from current file
            val currentFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE)
            if (currentFile == null) {
                showTimedNotification("$name No open file", "Cannot perform action when no file is opened")
                return
            }
            searchPath = currentFile.parent.path + "/" + location
        }

        val vfPath = getVirtualFileFromPath(searchPath)
        if (vfPath == null) {
            showTimedNotification("$name path not found", "Trying to open path ${searchPath}, but this path does not exist")
            return
        }

//        val changeListManager = if (settings.common.searchOnlyFilesTrackedByVersionControl) ChangeListManager.getInstance(project) else null
//        val allFiles = getAllFilesInRoot(vfPath, settings.common.excludedDirs, emptyList(), changeListManager)
//        files = allFiles.filter { vf -> regex.matches(vf.vf.name) }
//        files ?: return

        files = project.service<FileWatcher>().getListOfFiles(
            vfPath,
            project,
            settings.common.searchOnlyFilesTrackedByVersionControl,
            ::isFileIncluded)

        searchAction.doSearchForFiles(files!!, project, "", null)
    }

    fun getVirtualFileFromPath(filePath: String): VirtualFile? {
        val virtualFile = VfsUtil.findFile(Path(filePath), true)
        return virtualFile
    }

    fun isFileIncluded(vf: VirtualFile): Boolean {
        return regex.matches(vf.name)
    }

    companion object {
        fun getActionName(actionSettings: Array<String>) : String {
            return actionSettings[0]
        }
        fun getActionShortcut(actionSettings: Array<String>) : String {
            return if (actionSettings.size > 3) actionSettings[3] else ""
        }
    }
}