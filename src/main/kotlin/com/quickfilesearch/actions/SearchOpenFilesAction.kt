package com.quickfilesearch.actions

import com.quickfilesearch.settings.GlobalSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.quickfilesearch.*
import com.quickfilesearch.searchbox.PopupInstance
import com.quickfilesearch.searchbox.getAllFilesInRoot
import kotlin.io.path.Path

class SearchOpenFilesAction(val action: Array<String>,
                            val settings: GlobalSettings.SettingsState) : AnAction(getActionName(action))
{
    val name = action[0]
    val extension: String

    var files: List<VirtualFile>? = null
    var project: Project? = null
    var searchAction: SearchForFiles? = null

    init {
        extension = sanitizeExtension(action[1])
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        files = getAllOpenFiles(project)
        searchAction = SearchForFiles(files!!, settings, project);
    }

    companion object {
        fun getActionName(actionSettins: Array<String>) : String {
            return actionSettins[0]
        }
        fun getActionShortcut(actionSettins: Array<String>) : String {
            return actionSettins[2]
        }
    }

    private fun getAllOpenFiles(project: Project): List<VirtualFile> {
        val fileEditorManager = FileEditorManager.getInstance(project)
        return fileEditorManager.openFiles.toList()
    }
}