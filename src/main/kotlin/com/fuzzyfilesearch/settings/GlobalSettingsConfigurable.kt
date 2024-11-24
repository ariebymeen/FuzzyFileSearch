package com.fuzzyfilesearch.settings

import com.intellij.openapi.options.Configurable
import com.fuzzyfilesearch.actions.*
import javax.swing.JComponent

class GlobalSettingsConfigurable : Configurable {
    private lateinit var component: GlobalSettingsComponent
    private var settings = GlobalSettings().getInstance()

    override fun createComponent(): JComponent? {
        component = GlobalSettingsComponent()

        // TODO: Make this naming consistent
        component.excludedDirs.text = settings.state.excludedDirs.joinToString("\n")
        component.nofVisibleFilesInSearchViewSelector.value = settings.state.numberOfFilesInSearchView
        component.searchCaseSensitiviyCheckbox.isSelected = settings.state.searchCaseSensitivity
        component.pathDisplayDropdownBox.selectedItem = settings.state.filePathDisplayType
        component.searchBoxWidth.value = settings.state.searchPopupWidth
        component.searchBoxHeight.value = settings.state.searchPopupHeight
        component.searchBoxWidthPx.value = settings.state.searchPopupWidthPx
        component.searchBoxHeightPx.value = settings.state.searchPopupHeightPx
        component.minSizeEditorPx.value = settings.state.minSizeEditorPx
        component.scalePopupSizeWithIde.isSelected = settings.state.scaleWithIdeBounds
        component.searchBoxPosX.value = settings.state.horizontalPositionOnScreen
        component.searchBoxPosY.value = settings.state.verticalPositionOnScreen
        component.searchBarHeight.value = settings.state.searchBarHeight
        component.searchItemHeight.value = settings.state.searchItemHeight
        component.shrinkSearchAreaWithResults.isSelected = settings.state.shrinkViewDynamically
        component.searchOnlyFilesInVersionControlCheckbox.isSelected = settings.state.searchOnlyFilesInVersionControl
        component.openFilesSingleClick.isSelected = settings.state.openWithSingleClick

        component.showEditorPreviewCheckbox.isSelected  = settings.state.showEditorPreview
        component.editorPreviewLocation.selectedItem = settings.state.editorPreviewLocation
        component.editorSizeRatio.value = settings.state.editorSizeRatio

        component.openRelativeFileActionsTable.setData(settings.state.openRelativeFileActions)
        component.searchRelativeFileActionsTable.setData(settings.state.searchRelativeFileActions)
        component.searchPathActionsTable.setData(settings.state.searchPathActions)

        if (settings.state.searchRecentFilesActions.isNotEmpty()) {
            component.searchRecentFiles.setData(settings.state.searchRecentFilesActions)
        }
        if (settings.state.searchOpenFilesActions.isNotEmpty()) {
            component.searchOpenFiles.setData(settings.state.searchOpenFilesActions)
        }

        component.setEditorScalingFields()
        return component.panel
    }

    override fun isModified(): Boolean {
        val newSet = component.excludedDirs.text
            .split("\n")
            .filter { it.isNotBlank() }
            .toSet()

        val modified = settings.state.excludedDirs != newSet
                || settings.state.numberOfFilesInSearchView != component.nofVisibleFilesInSearchViewSelector.value
                || settings.state.searchCaseSensitivity != component.searchCaseSensitiviyCheckbox.isSelected
                || settings.state.searchOnlyFilesInVersionControl != component.searchOnlyFilesInVersionControlCheckbox.isSelected
                || settings.state.filePathDisplayType != (component.pathDisplayDropdownBox.selectedItem as PathDisplayType)
                || settings.state.searchPopupWidth != component.searchBoxWidth.value
                || settings.state.searchPopupHeight != component.searchBoxHeight.value
                || settings.state.searchPopupWidthPx != component.searchBoxWidthPx.value
                || settings.state.searchPopupHeightPx != component.searchBoxHeightPx.value
                || settings.state.minSizeEditorPx != component.minSizeEditorPx.value
                || settings.state.scaleWithIdeBounds != component.scalePopupSizeWithIde.isSelected
                || settings.state.horizontalPositionOnScreen != component.searchBoxPosX.value
                || settings.state.verticalPositionOnScreen != component.searchBoxPosY.value
                || settings.state.searchBarHeight != component.searchBarHeight.value
                || settings.state.searchItemHeight != component.searchItemHeight.value
                || settings.state.shrinkViewDynamically != component.shrinkSearchAreaWithResults.isSelected
                || settings.state.openWithSingleClick != component.openFilesSingleClick.isSelected
                || settings.state.showEditorPreview != component.showEditorPreviewCheckbox.isSelected
                || settings.state.editorPreviewLocation != component.editorPreviewLocation.selectedItem
                || settings.state.editorSizeRatio != component.editorSizeRatio.value
                || !isEqual(settings.state.openRelativeFileActions, component.openRelativeFileActionsTable.getData())
                || !isEqual(settings.state.searchRelativeFileActions, component.searchRelativeFileActionsTable.getData())
                || !isEqual(settings.state.searchPathActions, component.searchPathActionsTable.getData())
                || !isEqual(settings.state.searchRecentFilesActions, component.searchRecentFiles.getData())
                || !isEqual(settings.state.searchOpenFilesActions, component.searchOpenFiles.getData())

        if (modified) {
            val error = checkSettings(component)
            if (error != null) {
                // show error message
                component.warningText.text = error
                component.warningText.isVisible = true
                return false
            }
            component.warningText.isVisible = false
            component.setEditorScalingFields()
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
            unregisterActions(settings.state.searchRelativeFileActions, SearchRelativeFileAction::getActionName, SearchRelativeFileAction::getActionShortcut)
            settings.state.searchRelativeFileActions = component.searchRelativeFileActionsTable.getData()
            registerSearchRelativeFileActions(settings.state.searchRelativeFileActions, settings.state)
        }

        if (!isEqual(settings.state.searchPathActions, component.searchPathActionsTable.getData())) {
            unregisterActions(settings.state.searchPathActions, SearchFileInPathAction::getActionName, SearchFileInPathAction::getActionShortcut)
            settings.state.searchPathActions = component.searchPathActionsTable.getData()
            registerSearchFileInPathActions(settings.state.searchPathActions, settings.state)
        }

        if (!isEqual(settings.state.searchRecentFilesActions, component.searchRecentFiles.getData())) {
            unregisterActions(settings.state.searchRecentFilesActions, SearchRecentFilesAction::getActionName, SearchRecentFilesAction::getActionShortcut)
            settings.state.searchRecentFilesActions= component.searchRecentFiles.getData()
            registerSearchRecentFiles(settings.state.searchRecentFilesActions, settings.state)
        }

        if (!isEqual(settings.state.searchOpenFilesActions, component.searchOpenFiles.getData())) {
            unregisterActions(settings.state.searchOpenFilesActions, SearchOpenFilesAction::getActionName, SearchOpenFilesAction::getActionShortcut)
            settings.state.searchOpenFilesActions = component.searchOpenFiles.getData()
            registerSearchOpenFiles(settings.state.searchOpenFilesActions, settings.state)
        }

        val newSet = component.excludedDirs.text
            .split("\n")
            .filter { it.isNotBlank() }
            .toSet()

        settings.state.excludedDirs = newSet
        settings.state.numberOfFilesInSearchView = component.nofVisibleFilesInSearchViewSelector.value as Int
        settings.state.searchCaseSensitivity = component.searchCaseSensitiviyCheckbox.isSelected
        settings.state.searchOnlyFilesInVersionControl = component.searchOnlyFilesInVersionControlCheckbox.isSelected
        settings.state.filePathDisplayType = component.pathDisplayDropdownBox.selectedItem as PathDisplayType
        settings.state.searchPopupWidth = component.searchBoxWidth.value as Double
        settings.state.searchPopupHeight = component.searchBoxHeight.value as Double
        settings.state.searchPopupWidthPx = component.searchBoxWidthPx.value as Int
        settings.state.searchPopupHeightPx = component.searchBoxHeightPx.value as Int
        settings.state.minSizeEditorPx = component.minSizeEditorPx.value as Int
        settings.state.scaleWithIdeBounds = component.scalePopupSizeWithIde.isSelected
        settings.state.horizontalPositionOnScreen = component.searchBoxPosX.value as Double
        settings.state.verticalPositionOnScreen = component.searchBoxPosY.value as Double
        settings.state.searchBarHeight = component.searchBarHeight.value as Int
        settings.state.searchItemHeight = component.searchItemHeight.value as Int
        settings.state.shrinkViewDynamically = component.shrinkSearchAreaWithResults.isSelected
        settings.state.openWithSingleClick = component.openFilesSingleClick.isSelected
        settings.state.showEditorPreview = component.showEditorPreviewCheckbox.isSelected
        settings.state.editorPreviewLocation = component.editorPreviewLocation.selectedItem as EditorLocation
        settings.state.editorSizeRatio = component.editorSizeRatio.value as Double

        if (settings.state.showEditorPreview) {
            settings.state.shrinkViewDynamically = false
        }
    }

    override fun getDisplayName(): String {
        return "Quick File Search Settings"
    }

}