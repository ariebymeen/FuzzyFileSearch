package com.fuzzyfilesearch.services

import com.fuzzyfilesearch.settings.GlobalSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.MessageBusConnection
import kotlin.math.min

@Service(Service.Level.PROJECT)
class RecentFilesKeeper(private val project: Project) : FileEditorManagerListener {
    val settings = GlobalSettings().getInstance().state
    val connection: MessageBusConnection
    val historyList = mutableListOf<VirtualFile>()
    val historyLength = 200 // Max history length

    init {
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

    fun getRecentFiles(nofFiles: Int = historyLength): List<VirtualFile> {
        val nofFiles = min(nofFiles, historyList.size)
        if (historyList.isEmpty()) return emptyList()
        return historyList.subList(historyList.size - nofFiles, historyList.size - 1)
    }
}
