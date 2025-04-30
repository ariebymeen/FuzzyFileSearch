package com.fuzzyfilesearch.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class StringSearchSettingsConfigurable : Configurable {
    private lateinit var component: StringSearchSettingsComponent
    private var settings = GlobalSettings().getInstance()

    override fun createComponent(): JComponent? {
        component = StringSearchSettingsComponent(settings.state)
        component.keeper.components.forEach{component ->
            component.initialize()
        }

        return component.panel
    }

    override fun isModified(): Boolean {
        var modified = false
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
            }
        }

        return modified
    }

    override fun apply() {
        component.keeper.components.forEach{ component -> component.store() }
    }

    override fun getDisplayName(): String {
        return "FuzzyFileSearch Settings"
    }

}