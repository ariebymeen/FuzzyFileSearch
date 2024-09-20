package com.quickfilesearch.actions

import com.quickfilesearch.settings.GlobalSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.quickfilesearch.searchbox.PopupInstanceItem
import com.quickfilesearch.searchbox.SearchDialogCellRenderer
import javax.swing.Popup

class SearchOpenFilesAction(val action: Array<String>,
                            val settings: GlobalSettings.SettingsState) : AnAction(getActionName(action))
{
    val name = action[0]
    private val extensions: List<String> = extractExtensions(action[1])
    lateinit var files: List<PopupInstanceItem>
    private var searchAction: SearchForFiles? = null

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        files = getAllOpenFiles(project)
        searchAction = SearchForFiles(files, settings, project);
    }

    companion object {
        fun getActionName(actionSettins: Array<String>) : String {
            return actionSettins[0]
        }
        fun getActionShortcut(actionSettins: Array<String>) : String {
            return actionSettins[2]
        }
    }

    private fun getAllOpenFiles(project: Project): List<PopupInstanceItem> {
        val fileEditorManager = FileEditorManager.getInstance(project)
        return fileEditorManager.openFiles.toList().filter { file -> extensions.isEmpty() || extensions.contains(file.extension) }
//            .map{ file -> PopupInstanceItem(file, SearchDialogCellRenderer.getLabelHtml(file, settings.filePathDisplayType, project)) }
            .map{ file -> PopupInstanceItem(file, "") }
    }
}