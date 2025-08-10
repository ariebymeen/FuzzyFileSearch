package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.searchbox.getAllFilesInRoot
import com.fuzzyfilesearch.settings.GlobalSettings
import com.fuzzyfilesearch.showTimedNotification
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager

class SearchFilesWithPatternAction(
    val actionSettings: utils.ActionSettings,
    val globalSettings: GlobalSettings.SettingsState) : AnAction(actionSettings.name) {

    data class Settings(
        var path: String,
        var regex: Regex,
        var onlyVcsTracked: Boolean)

    val settings = parseSettings(actionSettings.generic)
    val searchAction = SearchForFiles(globalSettings)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val searchPath = getSearchPath(settings, project, e) ?: return
        val vfPath = utils.getVirtualFileFromPath(searchPath) ?: return

        val changeListManager = if (settings.onlyVcsTracked) ChangeListManager.getInstance(project) else null
        val allFiles = getAllFilesInRoot(vfPath, globalSettings.common.excludedDirs, emptyList(), changeListManager)
        val files = allFiles.filter { vf -> settings.regex.matches(vf.vf.name) }

        searchAction.search(files, project, null, searchPath, "File search (pattern)")
    }

    fun getSearchPath(settings: Settings, project: Project, e: AnActionEvent): String? {
        val searchPath: String
        if (settings.path.isEmpty() || settings.path[0] == '/') { // Search from project root
            searchPath = project.basePath + settings.path
        } else { // Search from current file
            val currentFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE)
            if (currentFile == null) {
                showTimedNotification(
                    "${actionSettings.name} no open file",
                    "Cannot perform action when no file is opened")
                return null
            }
            searchPath = currentFile.parent.path + "/" + settings.path
        }
        return searchPath
    }

    companion object {
        fun parseSettings(actionSettings: List<String>): Settings {
            val settings = Settings(
                path = actionSettings[0],
                regex = utils.parseRegex(actionSettings.getOrElse(1) { "" }),
                onlyVcsTracked = actionSettings.getOrElse(2) { "true" }.toBoolean())
            return settings
        }

        fun register(settings: utils.ActionSettings, globalSettings: GlobalSettings.SettingsState) {
            val action = SearchFilesWithPatternAction(settings, globalSettings)
            utils.registerAction(settings.name, settings.shortcut, action)
        }
    }
}