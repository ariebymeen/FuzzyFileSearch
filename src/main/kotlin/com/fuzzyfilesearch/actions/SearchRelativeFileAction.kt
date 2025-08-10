package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.renderers.FileInstanceItem
import com.fuzzyfilesearch.searchbox.getAllFilesInRoot
import com.fuzzyfilesearch.searchbox.getParentSatisfyingRegex
import com.fuzzyfilesearch.settings.GlobalSettings
import com.fuzzyfilesearch.showTimedNotification
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vcs.changes.ChangeListManager

class SearchRelativeFileAction(
    val actionSettings: utils.ActionSettings,
    val globalSettings: GlobalSettings.SettingsState) : AnAction(actionSettings.name) {

    data class Settings(
        val regex: Regex,
        val extensionList: List<String>,
        val onlyVcsTracked: Boolean)

    val settings = parseSettings(actionSettings.generic)
    val searchAction = SearchForFiles(globalSettings)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val currentFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE)
        if (currentFile == null) {
            showTimedNotification("${actionSettings.name} No open file", "Cannot perform action when no file is opened")
            return
        }

        val changeListManager = if (settings.onlyVcsTracked) ChangeListManager.getInstance(project) else null
        var files: List<FileInstanceItem>? = null
        var searchDirectory: String
        if (settings.regex.pattern.isEmpty()) {
            // If pattern is empty, list all files from current directory down
            files = getAllFilesInRoot(
                currentFile.parent,
                globalSettings.common.excludedDirs,
                settings.extensionList,
                changeListManager)
            searchDirectory = currentFile.parent.path
        } else {
            // Else try finding a file matching pattern
            val matchingFile =
                    getParentSatisfyingRegex(project, currentFile, settings.regex, globalSettings.common.excludedDirs)
            if (matchingFile == null) {
                showTimedNotification(
                    "${actionSettings.name} Could not find file",
                    "Could not find file satisfying regex ${settings.regex.pattern}")
                return
            }
            searchDirectory = matchingFile.parent.path
            files = getAllFilesInRoot(
                matchingFile.parent,
                globalSettings.common.excludedDirs,
                settings.extensionList,
                changeListManager)
        }
        searchAction.search(files, project, settings.extensionList, searchDirectory)
    }

    companion object {
        fun parseSettings(actionSettings: List<String>): Settings {
            val settings = Settings(
                regex = utils.parseRegex(actionSettings[0]),
                extensionList = utils.extractExtensions(actionSettings[1]),
                onlyVcsTracked = actionSettings.getOrElse(2) { "true" }.toBoolean())
            return settings
        }

        fun register(settings: utils.ActionSettings, globalSettings: GlobalSettings.SettingsState) {
            val action = SearchRelativeFileAction(settings, globalSettings)
            utils.registerAction(settings.name, settings.shortcut, action)
        }
    }
}