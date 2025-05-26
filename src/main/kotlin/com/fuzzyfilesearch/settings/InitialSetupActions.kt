package com.fuzzyfilesearch.settings

import com.fuzzyfilesearch.actions.*
import com.fuzzyfilesearch.searchbox.colorToHexWithAlpha
import com.fuzzyfilesearch.searchbox.initFzf
import com.fuzzyfilesearch.services.FileWatcher
import com.fuzzyfilesearch.services.PopupMediator
import com.fuzzyfilesearch.services.RecentFilesKeeper
import com.fuzzyfilesearch.services.TabKeyPostProcessor
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import javax.swing.UIManager

class ApplicationStartupSetup : ProjectActivity {

    override suspend fun execute(project: Project) {
        val globalSettings = GlobalSettings().getInstance()
        registerOpenRelativeFileActions(globalSettings.state.openRelativeFileActions, globalSettings.state)
        registerSearchRelativeFileActions(globalSettings.state.searchRelativeFileActions, globalSettings.state)
        registerSearchFileInPathActions(globalSettings.state.searchPathActions, globalSettings.state)
        registerSearchRecentFiles(globalSettings.state.searchRecentFilesActions, globalSettings.state)
        registerSearchOpenFiles(globalSettings.state.searchOpenFilesActions, globalSettings.state)
        registerSearchAllFiles(globalSettings.state.searchAllFilesActions, globalSettings.state)
        registerSearchFileMatchingPatternActions(globalSettings.state.searchFilesMatchingPatterActions, globalSettings.state)
        registerSearchForRegexInFiles(globalSettings.state.searchStringMatchingPatternActions, globalSettings.state)
        registerGrepInFilesActions(globalSettings.state.searchStringMatchingSubstringActions, globalSettings.state)

        if (globalSettings.state.common.fontSize == 0) {
            val scheme = EditorColorsManager.getInstance().globalScheme
            globalSettings.state.common.fontSize = scheme.editorFontSize
        }
        if (globalSettings.state.common.selectedColor.isEmpty()) {
            globalSettings.state.common.selectedColor = colorToHexWithAlpha(UIManager.getColor("List.selectionBackground"))
        }

        project.service<RecentFilesKeeper>()    // Initialize project service
        project.service<PopupMediator>()        // Initialize project service
        project.service<TabKeyPostProcessor>().registerProcessor()        // Initialize project service
//        project.service<FileWatcherService>()        // Initialize project service
        project.service<FileWatcher>().setSettings(globalSettings.state)


        initFzf()
    }
}