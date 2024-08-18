package com.quickfilesearch.actions

import com.quickfilesearch.settings.GlobalSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.quickfilesearch.*
import com.quickfilesearch.searchbox.PopupInstance
import com.quickfilesearch.searchbox.getAllFilesInRoot
import com.quickfilesearch.searchbox.getParentSatisfyingRegex

class SearchRelativeFileAction(val action: Array<String>,
                               val settings: GlobalSettings.SettingsState) : AnAction(getActionName(action))
{
    val regex = Regex(pattern = action[1], options = setOf(RegexOption.IGNORE_CASE))
    val name = getActionName(action)
    var files: List<VirtualFile>? = null
    var project: Project? = null
    var searchAction: SearchForFiles? = null
    val extension: String

    init {
        extension = sanitizeExtension(action[2])
    }

    override fun actionPerformed(e: AnActionEvent) {
        project = e.project ?: return

        val currentFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE)
        if (currentFile == null) {
            showTimedNotification("$name No open file", "Cannot perform action when no file is opened");
            return
        }

        if (regex.pattern.isEmpty()) {
            // If pattern is empty, list all files from current directory down
            files = getAllFilesInRoot(currentFile.parent, settings.excludedDirs, action[2])
        } else {
            // Else try finding a file matching pattern
            val matchingFile = getParentSatisfyingRegex(project!!, currentFile, regex, settings.excludedDirs)
            if (matchingFile == null) {
                showTimedNotification("$name Could not find file", "Could not find file satisfying regex ${regex.pattern}");
                return
            }
            files = getAllFilesInRoot(matchingFile.parent, settings.excludedDirs, extension)
        }
        files ?: return
        searchAction = SearchForFiles(files!!, settings, project!!);
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