package com.openrelativefile.Settings

import com.openrelativefile.Actions.registerOpenRelativeFileActions
import com.openrelativefile.isFzfAvailable
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.openrelativefile.Actions.registerSearchFileInPathActions
import com.openrelativefile.Actions.registerSearchRelativeFileActions

class InitialSetupActions : ProjectActivity {
    override suspend fun execute(project: Project) {
        val globalSettings = GlobalSettings().getInstance()
        registerOpenRelativeFileActions(globalSettings.state.openRelativeFileActions, globalSettings.state)
        registerSearchRelativeFileActions(globalSettings.state.searchRelativeFileActions, globalSettings.state)
        registerSearchFileInPathActions(globalSettings.state.searchPathActions, globalSettings.state)

        val projectSettings = ProjectSettings().getInstance()
        registerOpenRelativeFileActions(projectSettings.state.openRelativeFileActions, globalSettings.state)
        registerSearchRelativeFileActions(projectSettings.state.searchRelativeFileActions, globalSettings.state)
        registerSearchFileInPathActions(projectSettings.state.searchPathActions, globalSettings.state)

        val fzfAvailable = isFzfAvailable()
        if (!fzfAvailable) {
            globalSettings.state.useFzfForSearching = false // If fzf is not available, disable it
        }
        println("Fzf available: $fzfAvailable")
    }
}