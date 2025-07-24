package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.renderers.FileInstanceItem
import com.fuzzyfilesearch.settings.GlobalSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class SearchOpenFilesAction(
    settings: utils.ActionSettings,
    val globalSettings: GlobalSettings.SettingsState
                           ) : AnAction(settings.name) {

    data class Settings(val extensionList: List<String>)

    val settings = parseSettings(settings.generic)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val openFiles = utils.getAllOpenFiles(project)
        val files =
                openFiles.filter { file -> settings.extensionList.isEmpty() || settings.extensionList.contains(file.extension) }
                    .map { file -> FileInstanceItem(file) }
        SearchForFiles(globalSettings).search(files, project, settings.extensionList, "Open files")
    }

    companion object {
        fun parseSettings(actionSettings: List<String>): Settings {
            val settings = Settings(extensionList = utils.extractExtensions(actionSettings.getOrElse(0) { "" }))
            return settings
        }

        fun register(settings: utils.ActionSettings, globalSettings: GlobalSettings.SettingsState) {
            val action = SearchOpenFilesAction(settings, globalSettings)
            utils.registerAction(settings.name, settings.shortcut, action)
        }
    }
}