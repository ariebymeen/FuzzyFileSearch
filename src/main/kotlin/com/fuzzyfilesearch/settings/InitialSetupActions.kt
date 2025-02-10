package com.fuzzyfilesearch.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.fuzzyfilesearch.actions.*
import com.fuzzyfilesearch.searchbox.colorToHexWithAlpha
import com.fuzzyfilesearch.searchbox.hexToColorWithAlpha
import com.fuzzyfilesearch.searchbox.initFzf
import com.fuzzyfilesearch.services.PopupMediator
import com.fuzzyfilesearch.services.RecentFilesKeeper
import com.fuzzyfilesearch.services.TabKeyPostProcessor
import com.intellij.openapi.editor.colors.EditorColorsManager
import javax.swing.UIManager

class ApplicationStartupSetup : ProjectActivity {

    override suspend fun execute(project: Project) {
        val globalSettings = GlobalSettings().getInstance()
        registerQuickFileSearchActions(globalSettings.state.openRelativeFileActions, globalSettings.state)
        registerSearchRelativeFileActions(globalSettings.state.searchRelativeFileActions, globalSettings.state)
        registerSearchFileInPathActions(globalSettings.state.searchPathActions, globalSettings.state)
        registerSearchRecentFiles(globalSettings.state.searchRecentFilesActions, globalSettings.state)
        registerSearchOpenFiles(globalSettings.state.searchOpenFilesActions, globalSettings.state)
        registerSearchFileMatchingPatternActions(globalSettings.state.searchFilesMatchingPatterActions, globalSettings.state)

        if (globalSettings.state.fontSize == 0) {
            val scheme = EditorColorsManager.getInstance().globalScheme
            globalSettings.state.fontSize = scheme.editorFontSize
        }
        if (globalSettings.state.selectedColor.isEmpty()) {
            globalSettings.state.selectedColor = colorToHexWithAlpha(UIManager.getColor("List.selectionBackground"))
        }

        project.service<RecentFilesKeeper>()    // Initialize project service
        project.service<PopupMediator>()        // Initialize project service
        project.service<TabKeyPostProcessor>().registerProcessor()        // Initialize project service
        initFzf()
    }
}