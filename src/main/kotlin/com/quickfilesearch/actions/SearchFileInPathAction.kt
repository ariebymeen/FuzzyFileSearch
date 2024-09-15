package com.quickfilesearch.actions

import com.quickfilesearch.settings.GlobalSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.quickfilesearch.*
import com.quickfilesearch.searchbox.PopupInstance
import com.quickfilesearch.searchbox.getAllFilesInRoot
import kotlin.io.path.Path

class SearchFileInPathAction(val action: Array<String>,
                             val settings: GlobalSettings.SettingsState) : AnAction(getActionName(action))
{
    val name = action[0]
    val location = action[1]
    val extensions: List<String>

    var files: List<VirtualFile>? = null
    var project: Project? = null
    var searchAction: SearchForFiles? = null

    init {
        extensions = extractExtensions(action[2])
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val searchPath: String
        if (location[0] == '/') { // Search from project root
            searchPath = project.basePath + location
        } else { // Search from current file
            val currentFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE)
            if (currentFile == null) {
                showTimedNotification("$name No open file", "Cannot perform action when no file is opened");
                return
            }
            searchPath = currentFile.parent.path + "/" + location
        }

        val vfPath = getVirtualFileFromPath(searchPath)
        if (vfPath == null) {
            showTimedNotification("$name path not found", "Trying to open path ${searchPath}, but this path does not exist");
            return
        }

        files = getAllFilesInRoot(vfPath, settings.excludedDirs, extensions)
        searchAction = SearchForFiles(files!!, settings, project, searchPath, extensions)
    }

    fun getVirtualFileFromPath(filePath: String): VirtualFile? {
        val virtualFile = VfsUtil.findFile(Path(filePath), true)
        return virtualFile
    }

    companion object {
        fun getActionName(actionSettins: Array<String>) : String {
            return actionSettins[0]
        }
        fun getActionShortcut(actionSettins: Array<String>) : String {
            return actionSettins[3]
        }
    }
}