package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.settings.GlobalSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.fuzzyfilesearch.*
import com.fuzzyfilesearch.renderers.FileInstanceItem
import com.fuzzyfilesearch.searchbox.getAllFilesInRoot
import com.fuzzyfilesearch.services.FileWatcher
import com.intellij.openapi.components.service
import com.intellij.openapi.vcs.changes.ChangeListManager
import kotlin.io.path.Path
import kotlin.system.measureTimeMillis

class SearchFileInPathAction(val action: Array<String>,
                             val settings: GlobalSettings.SettingsState,
                             val overrideVscIgnore: Boolean = false) : AnAction(getActionName(action))
{
    val name = action[0]
    val location = action[1]
    val extensions: List<String>

    var files: List<FileInstanceItem>? = null
    var project: Project? = null
    var searchAction = SearchForFiles(settings)

    init {
        extensions = extractExtensions(action[2])
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val searchPath: String
        if (location.isEmpty() || location[0] == '/') { // Search from project root
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

//        val timeTaken = measureTimeMillis {
            val changeListManager = if (settings.common.searchOnlyFilesTrackedByVersionControl && !overrideVscIgnore) ChangeListManager.getInstance(project) else null
            files = getAllFilesInRoot(vfPath, settings.common.excludedDirs, extensions, changeListManager)
//            println("Found ${files?.size} files in ${timeTaken} ms")
//        }

        // TODO: Re-enable once tested
//        val time2 = measureTimeMillis {
//            files = project.service<FileWatcher>().getListOfFiles(vfPath, project,
//                settings.common.searchOnlyFilesTrackedByVersionControl && !overrideVscIgnore,
//                ::isFileIncluded)
//        }
//        println("Retrieved ${files?.size} files in ${time2} ms")
        searchAction.doSearchForFiles(files!!, project, searchPath, extensions)
    }

    fun getVirtualFileFromPath(filePath: String): VirtualFile? {
        val virtualFile = VfsUtil.findFile(Path(filePath), true)
        return virtualFile
    }

    fun isFileIncluded(vf: VirtualFile): Boolean {
        return extensions.isEmpty() || extensions.contains(vf.extension)
    }

    companion object {
        fun getActionName(actionSettings: Array<String>) : String {
            return actionSettings[0]
        }
        fun getActionShortcut(actionSettings: Array<String>) : String {
            return actionSettings[3]
        }
    }
}