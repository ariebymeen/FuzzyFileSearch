package com.quickfilesearch.settings

import com.intellij.openapi.options.Configurable
import com.quickfilesearch.actions.*
import javax.swing.JComponent

class GlobalSettingsConfigurable : Configurable {
    private lateinit var component: GlobalSettingsComponent
    private var settings = GlobalSettings().getInstance()

    override fun createComponent(): JComponent? {
        component = GlobalSettingsComponent()

        component.excludedDirs.text = settings.state.excludedDirs.joinToString("\n")
        component.nofVisibleFilesInSearchViewSelector.value = settings.state.numberOfFilesInSearchView
        component.useFzfCheckbox.isSelected = settings.state.useFzfForSearching
        component.pathDisplayDropdownBox.selectedItem = settings.state.filePathDisplayType
        component.searchBoxWidth.value = settings.state.searchPopupWidth
        component.searchBoxHeight.value = settings.state.searchPopupHeight

        component.openRelativeFileActionsTable.setData(settings.state.openRelativeFileActions)
        component.searchRelativeFileActionsTable.setData(settings.state.searchRelativeFileActions)
        component.searchPathActionsTable.setData(settings.state.searchPathActions)

        return component.panel
    }

    override fun isModified(): Boolean {
        val newSet = component.excludedDirs.text
            .split("\n")
            .filter { it.isNotBlank() }
            .toSet()

        val modified = settings.state.excludedDirs != newSet
                || settings.state.numberOfFilesInSearchView != component.nofVisibleFilesInSearchViewSelector.value
                || settings.state.useFzfForSearching != component.useFzfCheckbox.isSelected
                || settings.state.filePathDisplayType!= (component.pathDisplayDropdownBox.selectedItem as PathDisplayType)
                || settings.state.searchPopupWidth != component.searchBoxWidth.value
                || settings.state.searchPopupHeight != component.searchBoxHeight.value
                || !isEqual(settings.state.openRelativeFileActions, component.openRelativeFileActionsTable.getData())
                || !isEqual(settings.state.searchRelativeFileActions, component.searchRelativeFileActionsTable.getData())
                || !isEqual(settings.state.searchPathActions, component.searchPathActionsTable.getData())

        if (modified) {
            val error = checkSettings(component, null)
            if (error != null) {
                // show error message
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
            registerQuickFileSearchActions(settings.state.openRelativeFileActions, settings.state)
        }

        if (!isEqual(settings.state.searchRelativeFileActions, component.searchRelativeFileActionsTable.getData())) {
            unregisterActions(settings.state.openRelativeFileActions, SearchRelativeFileAction::getActionName, SearchRelativeFileAction::getActionShortcut)
            settings.state.searchRelativeFileActions = component.searchRelativeFileActionsTable.getData()
            registerSearchRelativeFileActions(settings.state.searchRelativeFileActions, settings.state)
        }

        if (!isEqual(settings.state.searchPathActions, component.searchPathActionsTable.getData())) {
            unregisterActions(settings.state.searchPathActions, SearchFileInPathAction::getActionName, SearchFileInPathAction::getActionShortcut)
            settings.state.searchPathActions = component.searchPathActionsTable.getData()
            registerSearchFileInPathActions(settings.state.searchPathActions, settings.state)
        }

        val newSet = component.excludedDirs.text
            .split("\n")
            .filter { it.isNotBlank() }
            .toSet()

        settings.state.excludedDirs = newSet
        settings.state.numberOfFilesInSearchView = component.nofVisibleFilesInSearchViewSelector.value as Int
        settings.state.useFzfForSearching = component.useFzfCheckbox.isSelected
        settings.state.filePathDisplayType = component.pathDisplayDropdownBox.selectedItem as PathDisplayType
        settings.state.searchPopupWidth = component.searchBoxWidth.value as Double
        settings.state.searchPopupHeight = component.searchBoxHeight.value as Double
    }

    override fun getDisplayName(): String {
        return "Global QuickFileSearch Settings"
    }

    // TODO: Perform some sensible validation on the shortcuts and the actions that you try to register
//    private fun validateInput() : Boolean {
//        component.openRelativeFileWarningLabel.isVisible = false
//
//        val shortcutsOpenActions = component.openRelativeFileActionsTable.getData().map { entry -> entry.shortcut }
//        val shortcutsSearchActions = component.searchRelativeFileActionsTable.getData().map { entry -> entry.shortcut }
//
//        val shortcuts = shortcutsOpenActions + shortcutsSearchActions;
//        if (shortcuts.size != shortcuts.distinct().size) {
//            component.openRelativeFileWarningLabel.text = "Warning some shortcuts are registered twice, this will give problems"
//            component.openRelativeFileWarningLabel.isVisible = true
//        }
//
//        return false
//    }
}