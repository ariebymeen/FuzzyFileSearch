package com.fuzzyfilesearch.settings

import com.fuzzyfilesearch.actions.ActionType
import com.fuzzyfilesearch.actions.utils.registerActionsFromSettings
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

        // TODO: REMOVE, this is legacy, used for porting old settings into the new structure only!
        try {
            globalSettings.state.allActions = globalSettings.state.allActions + globalSettings.state.openRelativeFileActions.map { arrayOf(ActionType.OPEN_RELATIVE_FILE.toString(), it[0], it[3], it[1], it[2]) }
            globalSettings.state.openRelativeFileActions = emptyArray()
            globalSettings.state.allActions = globalSettings.state.allActions + globalSettings.state.searchRelativeFileActions.map { arrayOf(ActionType.SEARCH_FILE_IN_RELATED_PATH.toString(), it[0], it[3], it[1], it[2], "true") }
            globalSettings.state.searchRelativeFileActions = emptyArray()
            globalSettings.state.allActions = globalSettings.state.allActions + globalSettings.state.searchPathActions.map { arrayOf(ActionType.SEARCH_FILE_IN_PATH.toString(), it[0], it[3], it[1], it[2], "true") }
            globalSettings.state.searchPathActions = emptyArray()
            globalSettings.state.allActions = globalSettings.state.allActions + globalSettings.state.searchFilesMatchingPatterActions.map { arrayOf(ActionType.SEARCH_FILE_MATCHING_PATTERN.toString(), it[0], it[3], it[1], it[2], "true") }
            globalSettings.state.searchFilesMatchingPatterActions = emptyArray()
            globalSettings.state.allActions = globalSettings.state.allActions + globalSettings.state.searchRecentFilesActions.map { arrayOf(ActionType.SEARCH_RECENT_FILES.toString(), it[0], it.getOrElse(3) {""}, it[1], it[2], "false", "true") }
            globalSettings.state.searchRecentFilesActions = emptyArray()
            globalSettings.state.allActions = globalSettings.state.allActions + globalSettings.state.searchOpenFilesActions.map { arrayOf(ActionType.SEARCH_RECENT_FILES.toString(), it[0], it.getOrElse(3) {""}, "0", it[2], "false", "true") }
            globalSettings.state.searchOpenFilesActions = emptyArray()
            globalSettings.state.allActions = globalSettings.state.allActions + globalSettings.state.searchAllFilesActions.map { arrayOf(ActionType.SEARCH_FILE_IN_PATH.toString(), it[0], it.getOrElse(2) { "" }, "/", it[1], "false") }
            globalSettings.state.searchAllFilesActions = emptyArray()
            globalSettings.state.allActions = globalSettings.state.allActions + globalSettings.state.searchStringMatchingPatternActions.map { arrayOf(ActionType.REGEX_SEARCH_IN_FILES.toString(), it[0], it[3], it[1], it[2], it[4], "true") }
            globalSettings.state.searchStringMatchingPatternActions = emptyArray()
            globalSettings.state.allActions = globalSettings.state.allActions + globalSettings.state.searchStringMatchingSubstringActions.map { arrayOf(ActionType.GREP_IN_FILES.toString(), it[0], it[3], it[1], it[2], "true") }
            globalSettings.state.searchStringMatchingSubstringActions = emptyArray()
        } catch (e: Exception) {
            println("Error copying old settings: ${e.message}")
        }
        // TODO: REMOVE END

        registerActionsFromSettings(globalSettings.state.allActions, globalSettings.state)

        if (globalSettings.state.common.fontSize == 0) {
            val scheme = EditorColorsManager.getInstance().globalScheme
            globalSettings.state.common.fontSize = scheme.editorFontSize
        }
        if (globalSettings.state.common.selectedColor.isEmpty()) {
            globalSettings.state.common.selectedColor =
                    colorToHexWithAlpha(UIManager.getColor("List.selectionBackground"))
        }

        project.service<RecentFilesKeeper>()    // Initialize project service
        project.service<PopupMediator>()        // Initialize project service
        project.service<TabKeyPostProcessor>().registerProcessor()        // Initialize project service
//        project.service<FileWatcherService>()        // Initialize project service
        project.service<FileWatcher>().setSettings(globalSettings.state)

        initFzf()
    }
}