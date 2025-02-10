package com.fuzzyfilesearch.settings

import com.intellij.openapi.options.Configurable
import com.fuzzyfilesearch.actions.*
import com.fuzzyfilesearch.searchbox.colorToHexWithAlpha
import com.fuzzyfilesearch.searchbox.hexToColorWithAlpha
import com.fuzzyfilesearch.showErrorNotification
import com.intellij.ui.ColorPanel
import com.intellij.ui.components.JBCheckBox
import java.awt.GraphicsEnvironment
import javax.swing.JComponent
import javax.swing.KeyStroke

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
        component.modifierKeyDropdownBox.selectedItem = settings.state.modifierKey
        component.openFileInVerticalSplitShortcutInputBox.text = settings.state.openInVerticalSplit
        component.openFileInHorizontalSplitShortcutInputBox.text = settings.state.openInHorizontalSplit
        component.openFileInActiveEditorShortcutInputBox.text = settings.state.openInActiveEditor
        component.searchBoxWidth.value = settings.state.searchPopupWidth
        component.searchBoxHeight.value = settings.state.searchPopupHeight
        component.searchBoxWidthPx.value = settings.state.searchPopupWidthPx
        component.searchBoxHeightPx.value = settings.state.searchPopupHeightPx
        component.minSizeEditorPx.value = settings.state.minSizeEditorPx
        component.popupSizePolicySelector.selectedItem = settings.state.popupSizePolicy
        component.searchBoxPosX.value = settings.state.horizontalPositionOnScreen
        component.searchBoxPosY.value = settings.state.verticalPositionOnScreen
        component.searchBarHeight.value = settings.state.searchBarHeight
        component.searchItemHeight.value = settings.state.searchItemHeight
        component.useDefaultFontCheckbox.isSelected = settings.state.useDefaultFont
        component.fontSelectorDropdown.fontName = settings.state.selectedFontName
        component.fontSize.value = settings.state.fontSize
        component.useDefaultHighlightColorCheckbox.isSelected = settings.state.useDefaultHighlightColor
        component.colorSelectorElement.selectedColor = hexToColorWithAlpha(settings.state.selectedColor)

        component.shrinkSearchAreaWithResults.isSelected = settings.state.shrinkViewDynamically
        component.searchOnlyFilesInVersionControlCheckbox.isSelected = settings.state.searchOnlyFilesInVersionControl
        component.openFilesSingleClick.isSelected = settings.state.openWithSingleClick

        component.showEditorPreviewCheckbox.isSelected = settings.state.showEditorPreview
        component.editorPreviewLocation.selectedItem = settings.state.editorPreviewLocation
        component.editorSizeRatio.value = settings.state.editorSizeRatio

        component.openRelativeFileActionsTable.setData(settings.state.openRelativeFileActions)
        component.searchFileMatchingPatternActionsTable.setData(settings.state.searchFilesMatchingPatterActions)
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
                || settings.state.modifierKey != (component.modifierKeyDropdownBox.selectedItem as ModifierKey)
                || settings.state.openInVerticalSplit != component.openFileInVerticalSplitShortcutInputBox.text
                || settings.state.openInHorizontalSplit != component.openFileInHorizontalSplitShortcutInputBox.text
                || settings.state.openInActiveEditor != component.openFileInActiveEditorShortcutInputBox.text
                || settings.state.searchPopupWidth != component.searchBoxWidth.value
                || settings.state.searchPopupHeight != component.searchBoxHeight.value
                || settings.state.searchPopupWidthPx != component.searchBoxWidthPx.value
                || settings.state.searchPopupHeightPx != component.searchBoxHeightPx.value
                || settings.state.minSizeEditorPx != component.minSizeEditorPx.value
                || settings.state.popupSizePolicy != (component.popupSizePolicySelector.selectedItem as PopupSizePolicy)
                || settings.state.horizontalPositionOnScreen != component.searchBoxPosX.value
                || settings.state.verticalPositionOnScreen != component.searchBoxPosY.value
                || settings.state.searchBarHeight != component.searchBarHeight.value
                || settings.state.searchItemHeight != component.searchItemHeight.value
                || settings.state.useDefaultFont != component.useDefaultFontCheckbox.isSelected
                || settings.state.selectedFontName != component.fontSelectorDropdown.fontName
                || settings.state.fontSize != component.fontSize.value
                || settings.state.useDefaultHighlightColor != component.useDefaultHighlightColorCheckbox.isSelected
                || settings.state.selectedColor != colorToHexWithAlpha(component.colorSelectorElement.selectedColor)
                || settings.state.shrinkViewDynamically != component.shrinkSearchAreaWithResults.isSelected
                || settings.state.openWithSingleClick != component.openFilesSingleClick.isSelected
                || settings.state.showEditorPreview != component.showEditorPreviewCheckbox.isSelected
                || settings.state.editorPreviewLocation != component.editorPreviewLocation.selectedItem
                || settings.state.editorSizeRatio != component.editorSizeRatio.value
                || !isEqual(settings.state.openRelativeFileActions, component.openRelativeFileActionsTable.getData())
                || !isEqual(settings.state.searchRelativeFileActions, component.searchRelativeFileActionsTable.getData())
//                || !isEqual(settings.state.openRelatedFileAction, component.open.getData())
                || !isEqual(settings.state.searchFilesMatchingPatterActions, component.searchFileMatchingPatternActionsTable.getData())
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
            } else {
                component.warningText.isVisible = false
                component.setEditorScalingFields()
            }
        }
        component.fontSelectorDropdown.isEnabled = !component.useDefaultFontCheckbox.isSelected
        component.colorSelectorElement.isEnabled = !component.useDefaultHighlightColorCheckbox.isSelected
        return modified
    }

    override fun apply() {
        var sc = component.openFileInVerticalSplitShortcutInputBox.text
        if (sc.isNotEmpty() && KeyStroke.getKeyStroke(sc) == null) {
            showErrorNotification("Invalid shortcut ${sc}", "Shortcut ${sc} is not valid and cannot be saved")
            return
        }
        sc = component.openFileInHorizontalSplitShortcutInputBox.text
        if (sc.isNotEmpty() && KeyStroke.getKeyStroke(sc) == null) {
            showErrorNotification("Invalid shortcut ${sc}", "Shortcut ${sc} is not valid and cannot be saved")
            return
        }

        if (!isEqual(settings.state.openRelativeFileActions, component.openRelativeFileActionsTable.getData())) {
            unregisterActions(
                settings.state.openRelativeFileActions,
                OpenRelativeFileAction::getActionName,
                OpenRelativeFileAction::getActionShortcut
            )
            settings.state.openRelativeFileActions = component.openRelativeFileActionsTable.getData()
            registerQuickFileSearchActions(settings.state.openRelativeFileActions, settings.state)
        }

        if (!isEqual(settings.state.searchRelativeFileActions, component.searchRelativeFileActionsTable.getData())) {
            unregisterActions(
                settings.state.searchRelativeFileActions,
                SearchRelativeFileAction::getActionName,
                SearchRelativeFileAction::getActionShortcut
            )
            settings.state.searchRelativeFileActions = component.searchRelativeFileActionsTable.getData()
            registerSearchRelativeFileActions(settings.state.searchRelativeFileActions, settings.state)
        }

        if (!isEqual(settings.state.searchPathActions, component.searchPathActionsTable.getData())) {
            unregisterActions(
                settings.state.searchPathActions,
                SearchFileInPathAction::getActionName,
                SearchFileInPathAction::getActionShortcut
            )
            settings.state.searchPathActions = component.searchPathActionsTable.getData()
            registerSearchFileInPathActions(settings.state.searchPathActions, settings.state)
        }

        if (!isEqual(settings.state.searchRecentFilesActions, component.searchRecentFiles.getData())) {
            unregisterActions(
                settings.state.searchRecentFilesActions,
                SearchRecentFilesAction::getActionName,
                SearchRecentFilesAction::getActionShortcut
            )
            settings.state.searchRecentFilesActions = component.searchRecentFiles.getData()
            registerSearchRecentFiles(settings.state.searchRecentFilesActions, settings.state)
        }

        if (!isEqual(settings.state.searchOpenFilesActions, component.searchOpenFiles.getData())) {
            unregisterActions(
                settings.state.searchOpenFilesActions,
                SearchOpenFilesAction::getActionName,
                SearchOpenFilesAction::getActionShortcut
            )
            settings.state.searchOpenFilesActions = component.searchOpenFiles.getData()
            registerSearchOpenFiles(settings.state.searchOpenFilesActions, settings.state)
        }

        if (!isEqual(
                settings.state.searchFilesMatchingPatterActions,
                component.searchFileMatchingPatternActionsTable.getData()
            )
        ) {
            unregisterActions(
                settings.state.searchFilesMatchingPatterActions,
                SearchFilesWithPatternAction::getActionName,
                SearchFilesWithPatternAction::getActionShortcut
            )
            settings.state.searchFilesMatchingPatterActions = component.searchFileMatchingPatternActionsTable.getData()
            registerSearchFileMatchingPatternActions(settings.state.searchFilesMatchingPatterActions, settings.state)
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
        settings.state.modifierKey = component.modifierKeyDropdownBox.selectedItem as ModifierKey
        settings.state.openInVerticalSplit = component.openFileInVerticalSplitShortcutInputBox.text
        settings.state.openInHorizontalSplit = component.openFileInHorizontalSplitShortcutInputBox.text
        settings.state.openInActiveEditor = component.openFileInActiveEditorShortcutInputBox.text
        settings.state.searchPopupWidth = component.searchBoxWidth.value as Double
        settings.state.searchPopupHeight = component.searchBoxHeight.value as Double
        settings.state.searchPopupWidthPx = component.searchBoxWidthPx.value as Int
        settings.state.searchPopupHeightPx = component.searchBoxHeightPx.value as Int
        settings.state.minSizeEditorPx = component.minSizeEditorPx.value as Int
        settings.state.popupSizePolicy = component.popupSizePolicySelector.selectedItem as PopupSizePolicy
        settings.state.horizontalPositionOnScreen = component.searchBoxPosX.value as Double
        settings.state.verticalPositionOnScreen = component.searchBoxPosY.value as Double
        settings.state.searchBarHeight = component.searchBarHeight.value as Int
        settings.state.searchItemHeight = component.searchItemHeight.value as Int
        settings.state.useDefaultFont = component.useDefaultFontCheckbox.isSelected
        settings.state.selectedFontName = component.fontSelectorDropdown.fontName as String
        settings.state.fontSize = component.fontSize.value as Int
        settings.state.useDefaultHighlightColor = component.useDefaultHighlightColorCheckbox.isSelected
        settings.state.selectedColor = colorToHexWithAlpha(component.colorSelectorElement.selectedColor)
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
        return "FuzzyFileSearch Settings"
    }

}