package com.openrelativefile.Actions

import com.openrelativefile.Settings.GlobalSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.openrelativefile.*
import kotlin.io.path.Path

class SearchFileInPathAction(val action: Array<String>,
                             val settings: GlobalSettings.SettingsState) : AnAction(getActionName(action))
{
    val name = action[0]
    val location = action[1]
    val extension: String

    var popup : PopupInstance? = null
    var files: List<VirtualFile>? = null
    var project: Project? = null
    var searchAction: SearchForFiles? = null

    init {
        if (action[2].isNotEmpty() && action[2][0] == '.') {
            extension = action[2].substring(1)
        } else {
            extension = action[2]
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        println("Action performed!")
        val project = e.project ?: return

        var searchPath = ""
        if (location[0] == '/') { // Search from project root
            searchPath = project.basePath + location
            println("Search from project root. Search path: $searchPath")
        } else { // Search from current file
            val currentFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE)
            if (currentFile == null) {
                showTimedNotification("$name No open file", "Cannot perform action when no file is opened");
                return
            }
            searchPath = currentFile.parent.path + "/" + location
            println("Search from current file. Search path: $searchPath")
        }

        val vfPath = getVirtualFileFromPath(searchPath)
        if (vfPath == null) {
            showTimedNotification("$name path not found", "Trying to open path ${searchPath}, but this path does not exist");
            return
        }
        files = getAllFilesInRoot(vfPath, settings.excludedDirs, extension)
        searchAction = SearchForFiles(files!!, settings, project);
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