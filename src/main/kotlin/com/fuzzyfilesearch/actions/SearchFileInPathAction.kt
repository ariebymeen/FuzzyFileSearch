package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.renderers.FileInstanceItem
import com.fuzzyfilesearch.searchbox.getAllFilesInRoot
import com.fuzzyfilesearch.settings.GlobalSettings
import com.fuzzyfilesearch.showTimedNotification
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import kotlin.system.measureTimeMillis

class SearchFileInPathAction(
    val actionSettings: utils.ActionSettings,
    val globalSettings: GlobalSettings.SettingsState) : AnAction(actionSettings.name) {

    data class Settings(
        var path: String,
        var extensionList: List<String>,
        var onlyVcsTracked: Boolean,
        var searchModifiedOnly: Boolean)

    val settings: Settings = parseSettings(actionSettings.generic)
    val searchForFiles = SearchForFiles(globalSettings)
    var totalTime: Long = 0
    var totalTimeCount: Long = 0

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val searchPath = getSearchPath(settings, project, e) ?: return
        val vfPath = utils.getVirtualFileFromPath(searchPath) ?: return

        var files: ArrayList<FileInstanceItem>
        val time = measureTimeMillis {
            val changeListManager = if (settings.onlyVcsTracked) ChangeListManager.getInstance(project) else null
            files = getAllFilesInRoot(
                vfPath,
                globalSettings.common.excludedDirs,
                settings.extensionList,
                changeListManager)

            if (settings.searchModifiedOnly) {
                files = ArrayList(files.filter { utils.isFileModifiedOrAdded(project, it.vf) })
            }
        }

        if (globalSettings.common.enableDebugOptions) {
            totalTime += time
            totalTimeCount += 1
            println("Average time searching for files: ${totalTime / totalTimeCount}")
        }

        val title = if (settings.searchModifiedOnly) "File search (modified only)" else "File search"
        searchForFiles.search(files, project, settings.extensionList, searchPath, title)
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
                extensionList = utils.extractExtensions(actionSettings.getOrElse(1) { "" }),
                onlyVcsTracked = actionSettings.getOrElse(2) { "true" }.toBoolean(),
                searchModifiedOnly = actionSettings.getOrElse(3) { "false" }.toBoolean())
            return settings
        }

        fun register(settings: utils.ActionSettings, globalSettings: GlobalSettings.SettingsState) {
            val action = SearchFileInPathAction(settings, globalSettings)
            utils.registerAction(settings.name, settings.shortcut, action)
        }
    }
}