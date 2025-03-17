package com.fuzzyfilesearch.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.fuzzyfilesearch.actions.*
import com.fuzzyfilesearch.searchbox.colorToHexWithAlpha
import com.fuzzyfilesearch.services.PopupMediator
import com.fuzzyfilesearch.services.RecentFilesKeeper
import com.fuzzyfilesearch.services.TabKeyPostProcessor
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.keymap.KeymapManager
import javax.swing.KeyStroke
import javax.swing.UIManager
import com.fuzzyfilesearch.searchbox.*

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
        registerGrepInFilesActions(globalSettings.state.searchStringMatchingPatternActions, globalSettings.state)

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
        initFzf()

        ActionManager.getInstance().registerAction("TEST", GrepInFiles(arrayOf("Name","",""), globalSettings.state))
        val keyStroke = KeyStroke.getKeyStroke("shift alt Y")
        val sc = KeyboardShortcut(keyStroke, null)
        KeymapManager.getInstance().activeKeymap.addShortcut("TEST", sc)
    }
}