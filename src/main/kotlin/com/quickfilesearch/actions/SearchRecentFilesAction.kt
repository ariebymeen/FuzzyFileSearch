package com.quickfilesearch.actions

import com.quickfilesearch.settings.GlobalSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.MessageBusConnection
import com.quickfilesearch.*
import kotlin.math.min


@Service(Service.Level.PROJECT)
class RecentFilesKeeper(private val project: Project): FileEditorManagerListener {
    val settings = GlobalSettings().getInstance().state
    var connection: MessageBusConnection
    var historyList = mutableListOf<VirtualFile>()
    var historyLength = 100 // Max history length

    init {
        println("Project level service initialized")
        connection = project.messageBus.connect()
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
    }

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        if (historyList.contains(file)) {
            historyList.remove(file)
        }
        while (historyList.size >= historyLength) {
            historyList.removeAt(0)
        }
        historyList.add(file)
    }

    fun getRecentFiles(nofFiles: Int) : List<VirtualFile> {
        val nofFiles = min(nofFiles, historyList.size)
        if (historyList.isEmpty()) return emptyList()
        return historyList.subList(historyList.size - nofFiles, historyList.size - 1)
    }
}

class SearchRecentFilesAction(val action: Array<String>,
                              val settings: GlobalSettings.SettingsState) : AnAction(getActionName(action))
{
    val name = action[0]
    var history: Int
    val extensions: List<String>
    var searchAction: SearchForFiles? = null

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
        searchAction = SearchForFiles(filteredFiles, settings, project)
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