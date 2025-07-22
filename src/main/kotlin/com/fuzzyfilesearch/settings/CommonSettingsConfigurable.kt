package com.fuzzyfilesearch.settings

import com.fuzzyfilesearch.searchbox.colorToHexWithAlpha
import com.fuzzyfilesearch.searchbox.hexToColorWithAlpha
import com.intellij.openapi.options.Configurable
import java.awt.Component
import javax.swing.JComponent

fun getComponentsOfType(components: Array<Component>, type: ActionType): List<FindInPathSettingsComponent> {
    return components.filter { component ->
        val settingsComp = component as? FindInPathSettingsComponent
        type == settingsComp?.getType()
    } as List<FindInPathSettingsComponent>
}

fun getSettingsOfType(components: Array<Component>, type: ActionType): Array<Array<String>> {
    return getComponentsOfType(components, type).map { component -> component.getStored() }.toTypedArray()
}

class CommonSettingsConfigurable : Configurable {
    private lateinit var component: CommonSettingsComponent
    private var settings = GlobalSettings().getInstance()

    override fun createComponent(): JComponent? {
        component = CommonSettingsComponent(settings.state)
        component.keeper.components.forEach{component ->
            component.initialize()
        }

        component.excludedDirs.text = settings.state.common.excludedDirs.joinToString("\n")
        component.fontSelectorDropdown.fontName = settings.state.common.selectedFontName
        component.colorSelectorElement.selectedColor = hexToColorWithAlpha(settings.state.common.selectedColor)

        return component.panel
    }

    override fun isModified(): Boolean {
        val newSet = component.excludedDirs.text
            .split("\n")
            .filter { it.isNotBlank() }
            .toSet()

        var modified = settings.state.common.excludedDirs != newSet
                || settings.state.common.selectedFontName != component.fontSelectorDropdown.fontName
                || settings.state.common.selectedColor != colorToHexWithAlpha(component.colorSelectorElement.selectedColor)

        component.keeper.components.forEach{component ->
            modified = modified || component.modified()
        }

        // TODO: Get shortcuts & action names
        component.actionsCollectionPanel.components.forEach { component ->
            val settingsComp = component as? FindInPathSettingsComponent ?: return@forEach
            modified = modified || settingsComp.isModified()
        }

        // TODO: Also set modified if the size of the array is no longer equal to the items
        modified = checkIfSizeIsModified(modified, ActionType.SEARCH_FILE_IN_PATH, settings.state.searchPathActions.size)

        return modified
    }

    override fun apply() {
        val newSet = component.excludedDirs.text
            .split("\n")
            .filter { it.isNotBlank() }
            .toSet()

        settings.state.common.excludedDirs = newSet
        settings.state.common.selectedFontName = component.fontSelectorDropdown.fontName as String
        settings.state.common.selectedColor = colorToHexWithAlpha(component.colorSelectorElement.selectedColor)

        component.keeper.components.forEach{ component -> component.store() }

        settings.state.searchPathActions                = getSettingsOfType(component.actionsCollectionPanel.components, ActionType.SEARCH_FILE_IN_PATH)
        settings.state.searchRelativeFileActions        = getSettingsOfType(component.actionsCollectionPanel.components, ActionType.SEARCH_FILE_IN_RELATIVE_PATH)
        settings.state.searchFilesMatchingPatterActions = getSettingsOfType(component.actionsCollectionPanel.components, ActionType.SEARCH_FILE_MATCHING_PATTERN)
        settings.state.searchRecentFilesActions         = getSettingsOfType(component.actionsCollectionPanel.components, ActionType.SEARCH_RECENT_FILES)
        // TODO: Search open should be merged with search recent
        settings.state.searchOpenFilesActions           = getSettingsOfType(component.actionsCollectionPanel.components, ActionType.SEARCH_OPEN_FILES)
        // TODO: searchAllFilesActions should be merged with searchPathActions
//        settings.state.searchAllFilesActions            = getSettingsOfType(component.actionsCollectionPanel.components, ActionType.SEARCH_OPEN_FILES)

    }

    override fun getDisplayName(): String {
        return "FuzzyFileSearch Settings"
    }

    fun checkIfSizeIsModified(modified: Boolean, type: ActionType, storedSize: Int): Boolean {
        return modified || getComponentsOfType(component.actionsCollectionPanel.components, type).size != storedSize
    }

}