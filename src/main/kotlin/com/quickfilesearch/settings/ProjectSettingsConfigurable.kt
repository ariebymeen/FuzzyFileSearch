package com.quickfilesearch.settings

import com.intellij.openapi.options.Configurable
import com.quickfilesearch.actions.*
import javax.swing.JComponent

class ProjectSettingsConfigurable : Configurable {
    private lateinit var component: ProjectSettingsComponent
    private var settings = ProjectSettings().getInstance()
    private var globalSettings = GlobalSettings().getInstance()

    override fun createComponent(): JComponent? {
        component = ProjectSettingsComponent()

        component.openRelativeFileActionsTable.setData(settings.state.openRelativeFileActions)
        component.searchRelativeFileActionsTable.setData(settings.state.searchRelativeFileActions)
        component.searchPathActionsTable.setData(settings.state.searchPathActions)

        return component.panel
    }

    override fun isModified(): Boolean {
        val modified = !isEqual(settings.state.openRelativeFileActions, component.openRelativeFileActionsTable.getData())
                || !isEqual(settings.state.searchRelativeFileActions, component.searchRelativeFileActionsTable.getData())
                || !isEqual(settings.state.searchPathActions, component.searchPathActionsTable.getData())

        if (modified) {
            val error = checkSettings(null, component)
            if (error != null) {
                component.warningText.text = error
                component.warningText.isVisible = true
                return false
            }
            component.warningText.isVisible = false
        }
        return modified
    }

    override fun apply() {
        if (!isEqual(settings.state.openRelativeFileActions, component.openRelativeFileActionsTable.getData())) {
            unregisterActions(settings.state.openRelativeFileActions, QuickFileSearchAction::getActionName, QuickFileSearchAction::getActionShortcut)
            settings.state.openRelativeFileActions = component.openRelativeFileActionsTable.getData()
            registerQuickFileSearchActions(settings.state.openRelativeFileActions, globalSettings.state)
        }

        if (!isEqual(settings.state.searchRelativeFileActions, component.searchRelativeFileActionsTable.getData())) {
            unregisterActions(settings.state.openRelativeFileActions, SearchRelativeFileAction::getActionName, SearchRelativeFileAction::getActionShortcut)
            settings.state.searchRelativeFileActions = component.searchRelativeFileActionsTable.getData()
            registerSearchRelativeFileActions(settings.state.searchRelativeFileActions, globalSettings.state)
        }

        if (!isEqual(settings.state.searchPathActions, component.searchPathActionsTable.getData())) {
            unregisterActions(settings.state.searchPathActions, SearchFileInPathAction::getActionName, SearchFileInPathAction::getActionShortcut)
            settings.state.searchPathActions = component.searchPathActionsTable.getData()
            registerSearchFileInPathActions(settings.state.searchPathActions, globalSettings.state)
        }
    }

    override fun getDisplayName(): String {
        return "Project specific QuickFileSearch Settings"
    }
}