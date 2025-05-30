package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.settings.GlobalSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.fuzzyfilesearch.*
import com.fuzzyfilesearch.renderers.FileInstanceItem
import com.fuzzyfilesearch.searchbox.getAllFilesInRoot
import com.fuzzyfilesearch.searchbox.getParentSatisfyingRegex

class SearchRelativeFileAction(val action: Array<String>,
                               val settings: GlobalSettings.SettingsState) : AnAction(getActionName(action))
{
    val regex = Regex(pattern = action[1], options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    val name = getActionName(action)
    var files: List<FileInstanceItem>? = null
    var project: Project? = null
    var searchAction = SearchForFiles(settings)
    val extensions: List<String> = extractExtensions(action[2])

    override fun actionPerformed(e: AnActionEvent) {
        project = e.project ?: return

        val currentFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE)
        if (currentFile == null) {
            showTimedNotification("$name No open file", "Cannot perform action when no file is opened");
            return
        }

        val changeListManager = if (settings.common.searchOnlyFilesTrackedByVersionControl) ChangeListManager.getInstance(project!!) else null
        var directory = currentFile.parent
        if (regex.pattern.isEmpty()) {
            // If pattern is empty, list all files from current directory down
            files = getAllFilesInRoot(currentFile.parent, settings.common.excludedDirs, extensions, changeListManager)
        } else {
            // Else try finding a file matching pattern
            val matchingFile = getParentSatisfyingRegex(project!!, currentFile, regex, settings.common.excludedDirs)
            if (matchingFile == null) {
                showTimedNotification("$name Could not find file", "Could not find file satisfying regex ${regex.pattern}");
                return
            }
            directory = matchingFile.parent
            files = getAllFilesInRoot(matchingFile.parent, settings.common.excludedDirs, extensions, changeListManager)
        }
        files ?: return
        searchAction.doSearchForFiles(files!!, project!!, directory.path, extensions)
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