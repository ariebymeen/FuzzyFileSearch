package com.quickfilesearch.actions

import com.quickfilesearch.settings.GlobalSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.quickfilesearch.*
import com.quickfilesearch.searchbox.PopupInstanceItem
import com.quickfilesearch.services.RecentFilesKeeper

class SearchRecentFilesAction(val action: Array<String>,
                              val settings: GlobalSettings.SettingsState) : AnAction(getActionName(action))
{
    val name = action[0]
    var history: Int
    val extensions: List<String>
    var searchAction = SearchForFiles(settings)

    init {
        try {
            history = action[1].toInt()
        } catch (e: Exception) {
            showErrorNotification("Not a valid number", "Trying to register $name, but the history field was not set with a valid number. Using the default of 10")
            history = 10;
        }

        extensions = extractExtensions(action[2])
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val recentFiles = project.service<RecentFilesKeeper>().getRecentFiles(history).toMutableList()
        val openFiles = getAllOpenFiles(project).toMutableList()
        for (file in openFiles) {
            if (!recentFiles.contains(file)) {
                recentFiles += file
            }
        }
        val filteredFiles = recentFiles.filter { file -> extensions.isEmpty() || extensions.contains(file.extension) }
                                        .map{ file -> PopupInstanceItem(file) }
        searchAction.doSearchForFiles(filteredFiles, project, null, extensions)
    }

    companion object {
        fun getActionName(actionSettins: Array<String>) : String {
            return actionSettins[0]
        }
        fun getActionShortcut(actionSettins: Array<String>) : String {
            return actionSettins[3]
        }
    }

    private fun getAllOpenFiles(project: Project): List<VirtualFile> {
        val fileEditorManager = FileEditorManager.getInstance(project)
        return fileEditorManager.openFiles.toList()
    }

}