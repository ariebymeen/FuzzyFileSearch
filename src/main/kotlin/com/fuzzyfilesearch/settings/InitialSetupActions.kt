package com.fuzzyfilesearch.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.fuzzyfilesearch.actions.*
import com.fuzzyfilesearch.searchbox.initFzf
import com.fuzzyfilesearch.services.RecentFilesKeeper

class ApplicationStartupSetup : ProjectActivity {

    override suspend fun execute(project: Project) {
        println("Starting initial application setup")

        val globalSettings = GlobalSettings().getInstance()
        registerQuickFileSearchActions(globalSettings.state.openRelativeFileActions, globalSettings.state)
        registerSearchRelativeFileActions(globalSettings.state.searchRelativeFileActions, globalSettings.state)
        registerSearchFileInPathActions(globalSettings.state.searchPathActions, globalSettings.state)
        registerSearchRecentFiles(globalSettings.state.searchRecentFilesActions, globalSettings.state)
        registerSearchOpenFiles(globalSettings.state.searchOpenFilesActions, globalSettings.state)
        registerSearchFileMatchingPatternActions(globalSettings.state.searchFilesMatchingPatterActions, globalSettings.state)

        project.service<RecentFilesKeeper>() // Initialize project service
        initFzf()
    }
}