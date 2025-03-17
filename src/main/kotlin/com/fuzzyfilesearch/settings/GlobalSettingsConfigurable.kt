package com.fuzzyfilesearch.settings

import com.intellij.openapi.options.Configurable
import com.fuzzyfilesearch.actions.*
import com.fuzzyfilesearch.searchbox.colorToHexWithAlpha
import com.fuzzyfilesearch.searchbox.hexToColorWithAlpha
import com.fuzzyfilesearch.showErrorNotification
import javax.swing.JComponent
import javax.swing.KeyStroke

class GlobalSettingsConfigurable : Configurable {
    private lateinit var component: GlobalSettingsComponent
    private var settings = GlobalSettings().getInstance()

    override fun createComponent(): JComponent? {
        component = GlobalSettingsComponent(settings.state)

//        component.checkboxes.forEach{checkbox ->
//            checkbox.initialize()
//        }
        component.components.forEach{component ->
            component.initialize()
        }
        // TODO: Make this naming consistent
        component.excludedDirs.text = settings.state.excludedDirs.joinToString("\n")
//        component.nofVisibleFilesInSearchViewSelector.value = settings.state.numberOfFilesInSearchView
//        component.searchCaseSensitiviyCheckbox.isSelected = settings.state.searchCaseSensitivity
//        component.pathDisplayDropdownBox.selectedItem = settings.state.filePathDisplayType
//        component.modifierKeyDropdownBox.selectedItem = settings.state.modifierKey
//        component.openFileInVerticalSplitShortcutInputBox.text = settings.state.openInVerticalSplit
//        component.openFileInHorizontalSplitShortcutInputBox.text = settings.state.openInHorizontalSplit
//        component.openFileInActiveEditorShortcutInputBox.text = settings.state.openInActiveEditor
//        component.searchBoxWidth.value = settings.state.searchPopupWidth
//        component.searchBoxHeight.value = settings.state.searchPopupHeight
//        component.searchBoxWidthPx.value = settings.state.searchPopupWidthPx
//        component.searchBoxHeightPx.value = settings.state.searchPopupHeightPx
//        component.minSizeEditorPx.value = settings.state.minSizeEditorPx
//        component.popupSizePolicySelector.selectedItem = settings.state.popupSizePolicy
//        component.searchBoxPosX.value = settings.state.horizontalPositionOnScreen
//        component.searchBoxPosY.value = settings.state.verticalPositionOnScreen
//        component.searchBarHeight.value = settings.state.searchBarHeight
//        component.searchItemHeight.value = settings.state.searchItemHeight
//        component.useDefaultFontCheckbox.isSelected = settings.state.useDefaultFont
//        component.showNumberInSearchView.isSelected = settings.state.showNumberInSearchView
        component.fontSelectorDropdown.fontName = settings.state.selectedFontName
//        component.fontSize.value = settings.state.fontSize
//        component.useDefaultHighlightColorCheckbox.isSelected = settings.state.useDefaultHighlightColor
        component.colorSelectorElement.selectedColor = hexToColorWithAlpha(settings.state.selectedColor)

//        component.shrinkSearchAreaWithResults.isSelected = settings.state.shrinkViewDynamically
//        component.searchOnlyFilesInVersionControlCheckbox.isSelected = settings.state.searchOnlyFilesInVersionControl
//        component.openFilesSingleClick.isSelected = settings.state.openWithSingleClick

//        component.showEditorPreviewCheckbox.isSelected = settings.state.showEditorPreview
//        component.editorPreviewLocation.selectedItem = settings.state.editorPreviewLocation
//        component.editorSizeRatio.value = settings.state.editorSizeRatio
//        component.applySyntaxHighlightingOnTextSearchCheckbox.isSelected = settings.state.applySyntaxHighlightingOnTextSearch
//        component.showEditorPreviewStringSearch.isSelected = settings.state.showEditorPreviewStringSearch

//        component.openRelativeFileActionsTable.setData(settings.state.openRelativeFileActions)
//        component.searchFileMatchingPatternActionsTable.setData(settings.state.searchFilesMatchingPatterActions)
//        component.searchRelativeFileActionsTable.setData(settings.state.searchRelativeFileActions)
//        component.searchPathActionsTable.setData(settings.state.searchPathActions)
//        component.searchStringMatchingPattern.setData(settings.state.searchStringMatchingPatternActions)

//        if (settings.state.searchRecentFilesActions.isNotEmpty()) {
//            component.searchRecentFiles.setData(settings.state.searchRecentFilesActions)
//        }
//        if (settings.state.searchOpenFilesActions.isNotEmpty()) {
//            component.searchOpenFiles.setData(settings.state.searchOpenFilesActions)
//        }
//        if (settings.state.searchAllFilesActions.isNotEmpty()) {
//            component.searchAllFiles.setData(settings.state.searchAllFilesActions)
//        }

        component.setEditorScalingFields()
        return component.panel
    }

    override fun isModified(): Boolean {
        val newSet = component.excludedDirs.text
            .split("\n")
            .filter { it.isNotBlank() }
            .toSet()

        var modified = settings.state.excludedDirs != newSet
                || settings.state.selectedFontName != component.fontSelectorDropdown.fontName
                || settings.state.selectedColor != colorToHexWithAlpha(component.colorSelectorElement.selectedColor)
        component.components.forEach{component ->
            modified = modified || component.modified()
        }

        if (modified) {
            val error = checkSettings(component.components)
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

        return modified
    }

    override fun apply() {
        val newSet = component.excludedDirs.text
            .split("\n")
            .filter { it.isNotBlank() }
            .toSet()

        settings.state.excludedDirs = newSet
        settings.state.selectedFontName = component.fontSelectorDropdown.fontName as String
        settings.state.selectedColor = colorToHexWithAlpha(component.colorSelectorElement.selectedColor)

        component.components.forEach{ component -> component.store() }

        if (settings.state.showEditorPreview) {
            settings.state.shrinkViewDynamically = false
        }
    }

    override fun getDisplayName(): String {
        return "FuzzyFileSearch Settings"
    }

}