package com.quickfilesearch.actions

import com.quickfilesearch.settings.GlobalSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.ProjectManagerScope
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.MessageBusConnection
import com.quickfilesearch.*
import com.quickfilesearch.searchbox.PopupInstance
import com.quickfilesearch.searchbox.getAllFilesInRoot
import kotlin.io.path.Path

class RecentFilesKeeper(private val historyLength: Int) : FileEditorManagerListener {
    var history = mutableListOf<VirtualFile>()

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        super.fileOpened(source, file)
        println("File opened: ${file.name}")

        if (history.contains(file)) {
            history.remove(file)
        }
        if (history.size >= historyLength) {
            history.removeAt(0)
        }
        history.add(file)
    }
}

class SearchRecentFilesAction(val action: Array<String>,
                              val settings: GlobalSettings.SettingsState) : AnAction(getActionName(action)), AutoCloseable
{
    val name = action[0]
    var history: Int
    val extension: String

    var files: List<VirtualFile>? = null
    var project: Project? = null
    var searchAction: SearchForFiles? = null
    var recentFilesKeeper: RecentFilesKeeper
    var connection: MessageBusConnection? = null

    init {
        try {
            history = action[1].toInt()
        } catch (e: Exception) {
            showErrorNotification("Not a valid number", "Trying to register $name, but the history field was not set with a valid number. Using the default of 10")
            history = 10;
        }

        extension = sanitizeExtension(action[2])
        recentFilesKeeper = RecentFilesKeeper(history)

        project = ProjectManager.getInstance().openProjects.firstOrNull()
        if (project == null) {
            showErrorNotification("Could not get project", "$name: Error getting project. Searching for recent files will not work correctly")
        } else {
            connection = project!!.messageBus.connect()
            connection!!.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, recentFilesKeeper)
        }
    }

    override fun close() {
        if (connection != null) {
            connection!!.disconnect()
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        files = getAllOpenFiles(project) + recentFilesKeeper.history
        searchAction = SearchForFiles(files!!, settings, project);
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