package com.quickfilesearch.settings

import com.intellij.openapi.components.service
import com.quickfilesearch.searchbox.isFzfAvailable
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.quickfilesearch.actions.*
import com.quickfilesearch.searchbox.initFzf
import com.quickfilesearch.services.RecentFilesKeeper

class ApplicationStartupSetup : ProjectActivity {

    override suspend fun execute(project: Project) {
        println("Starting initial application setup")

        val globalSettings = GlobalSettings().getInstance()
        registerQuickFileSearchActions(globalSettings.state.openRelativeFileActions, globalSettings.state)
        registerSearchRelativeFileActions(globalSettings.state.searchRelativeFileActions, globalSettings.state)
        registerSearchFileInPathActions(globalSettings.state.searchPathActions, globalSettings.state)
        registerSearchRecentFiles(globalSettings.state.searchRecentFilesActions, globalSettings.state)
        registerSearchOpenFiles(globalSettings.state.searchOpenFilesActions, globalSettings.state)

        project.service<RecentFilesKeeper>() // Initialize project service
        initFzf("path")
    }
}