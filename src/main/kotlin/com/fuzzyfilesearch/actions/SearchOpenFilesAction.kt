package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.settings.GlobalSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.fuzzyfilesearch.searchbox.PopupInstanceItem

class SearchOpenFilesAction(val action: Array<String>,
                            val settings: GlobalSettings.SettingsState) : AnAction(getActionName(action))
{
    val name = action[0]
    private val extensions: List<String> = extractExtensions(action[1])
    lateinit var files: List<PopupInstanceItem>
    private var searchAction = SearchForFiles(settings)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        files = getAllOpenFiles(project)
        searchAction.doSearchForFiles(files, project, null, null)
    }

    companion object {
        fun getActionName(actionSettings: Array<String>) : String {
            return actionSettings[0]
        }
        fun getActionShortcut(actionSettings: Array<String>) : String {
            return actionSettings[2]
        }
    }

    private fun getAllOpenFiles(project: Project): List<PopupInstanceItem> {
        val fileEditorManager = FileEditorManager.getInstance(project)
        return fileEditorManager.openFiles.toList().filter { file -> extensions.isEmpty() || extensions.contains(file.extension) }
            .map{ file -> PopupInstanceItem(file) }
    }
}