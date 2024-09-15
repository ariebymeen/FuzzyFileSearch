package com.quickfilesearch.settings

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.quickfilesearch.searchbox.isFzfAvailable
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.startup.StartupActivity
import com.quickfilesearch.actions.*
import com.quickfilesearch.services.RecentFilesKeeper
import com.quickfilesearch.services.FileChangeListener

class ApplicationStartupSetup : ProjectActivity {

    override suspend fun execute(project: Project) {
        println("Starting initial application setup")

        val globalSettings = GlobalSettings().getInstance()
        registerQuickFileSearchActions(globalSettings.state.openRelativeFileActions, globalSettings.state)
        registerSearchRelativeFileActions(globalSettings.state.searchRelativeFileActions, globalSettings.state)
        registerSearchFileInPathActions(globalSettings.state.searchPathActions, globalSettings.state)
        registerSearchRecentFiles(globalSettings.state.searchRecentFilesActions, globalSettings.state)
        registerSearchOpenFiles(globalSettings.state.searchOpenFilesActions, globalSettings.state)

        val fzfAvailable = isFzfAvailable()
        if (!fzfAvailable) {
            globalSettings.state.useFzfForSearching = false // If fzf is not available, disable it
        }
        println("Fzf available: $fzfAvailable")

        project.service<RecentFilesKeeper>() // Initialize project service
        project.service<FileChangeListener>() // Initialize project service
    }
}