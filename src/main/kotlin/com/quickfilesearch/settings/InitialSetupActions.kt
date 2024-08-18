package com.quickfilesearch.settings

import com.quickfilesearch.searchbox.isFzfAvailable
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.quickfilesearch.actions.*

class InitialSetupActions : ProjectActivity {
    override suspend fun execute(project: Project) {
        val globalSettings = GlobalSettings().getInstance()
        registerQuickFileSearchActions(globalSettings.state.openRelativeFileActions, globalSettings.state)
        registerSearchRelativeFileActions(globalSettings.state.searchRelativeFileActions, globalSettings.state)
        registerSearchFileInPathActions(globalSettings.state.searchPathActions, globalSettings.state)
        registerSearchRecentFiles(globalSettings.state.searchRecentFilesActions, globalSettings.state)
        registerSearchOpenFiles(globalSettings.state.searchOpenFilesActions, globalSettings.state)

        val projectSettings = ProjectSettings().getInstance()
        registerQuickFileSearchActions(projectSettings.state.openRelativeFileActions, globalSettings.state)
        registerSearchRelativeFileActions(projectSettings.state.searchRelativeFileActions, globalSettings.state)
        registerSearchFileInPathActions(projectSettings.state.searchPathActions, globalSettings.state)

        val fzfAvailable = isFzfAvailable()
        if (!fzfAvailable) {
            globalSettings.state.useFzfForSearching = false // If fzf is not available, disable it
        }
        println("Fzf available: $fzfAvailable")
    }
}