package com.fuzzyfilesearch.settings

import com.fuzzyfilesearch.searchbox.colorToHexWithAlpha
import com.fuzzyfilesearch.searchbox.hexToColorWithAlpha
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class GlobalSettingsConfigurable : Configurable {
    private lateinit var component: GlobalSettingsComponent
    private var settings = GlobalSettings().getInstance()

    override fun createComponent(): JComponent? {
        component = GlobalSettingsComponent(settings.state)
        component.keeper.components.forEach{component ->
            component.initialize()
        }

        component.excludedDirs.text = settings.state.common.excludedDirs.joinToString("\n")
        component.fontSelectorDropdown.fontName = settings.state.common.selectedFontName
        component.colorSelectorElement.selectedColor = hexToColorWithAlpha(settings.state.common.selectedColor)
//        component.setEditorScalingFields()

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

        if (modified) {
            val error = checkSettings(component.keeper.components)
            if (error != null) {
                // show error message
                component.warningText.text = error
                component.warningText.isVisible = true
                return false
            } else {
                component.warningText.isVisible = false
//                component.setEditorScalingFields() // TODO
            }
        }

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

        if (settings.state.file.showEditorPreview) {
            settings.state.file.shrinkViewDynamically = false
        }
    }

    override fun getDisplayName(): String {
        return "FuzzyFileSearch Settings"
    }

}