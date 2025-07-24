package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.renderers.FileInstanceItem
import com.fuzzyfilesearch.services.RecentFilesKeeper
import com.fuzzyfilesearch.settings.GlobalSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.FileStatusManager
import com.intellij.openapi.vfs.VirtualFile
import kotlin.math.min

class SearchRecentFilesAction(
    settings: utils.ActionSettings,
    val globalSettings: GlobalSettings.SettingsState
                             ) : AnAction(settings.name) {

    data class Settings(
        var nofFilesHistory: Int,
        var extensionList: List<String>,
        var searchModifiedOnly: Boolean,
        var alwaysIncludeOpenFiles: Boolean)

    val settings = parseSettings(settings.generic)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val recentFiles = project.service<RecentFilesKeeper>().getRecentFiles().toMutableList()

        if (settings.alwaysIncludeOpenFiles) {
            val openFiles = utils.getAllOpenFiles(project).toMutableList()
            recentFiles += openFiles.filter { !recentFiles.contains(it) }
        }

        var filteredFiles = recentFiles.filter { file -> isFileExtensionIncluded(file) }
        if (settings.searchModifiedOnly) {
            filteredFiles = filteredFiles.filter { isFileModifiedOrAdded(project, it) }
        }
        filteredFiles = filteredFiles.takeLast(min(filteredFiles.size, settings.nofFilesHistory))

        val searchItems = filteredFiles.map { file -> FileInstanceItem(file) }
        val title = if (settings.searchModifiedOnly) "Recent files (edited only)" else "Recent files"
        SearchForFiles(globalSettings).search(searchItems, project, settings.extensionList, title)
    }

    fun isFileModifiedOrAdded(project: Project, vf: VirtualFile): Boolean {
        val status = FileStatusManager.getInstance(project).getStatus(vf)
        // It seems that new (not yet added files) are marked as UNKNOWN
        return status == FileStatus.MODIFIED || status == FileStatus.ADDED || status == FileStatus.UNKNOWN
    }

    fun isFileExtensionIncluded(vf: VirtualFile): Boolean {
        return settings.extensionList.isEmpty() || settings.extensionList.contains(vf.extension)
    }

    companion object {
        fun parseSettings(actionSettings: List<String>): Settings {
            val settings = Settings(
                nofFilesHistory = actionSettings.getOrElse(0) { "10" }.toInt(),
                extensionList = utils.extractExtensions(actionSettings.getOrElse(1) { "" }),
                searchModifiedOnly = actionSettings.getOrElse(2) { "true" }.toBoolean(),
                alwaysIncludeOpenFiles = actionSettings.getOrElse(3) { "true" }.toBoolean())
            return settings
        }

        fun register(settings: utils.ActionSettings, globalSettings: GlobalSettings.SettingsState) {
            val action = SearchRecentFilesAction(settings, globalSettings)
            utils.registerAction(settings.name, settings.shortcut, action)
        }
    }
}