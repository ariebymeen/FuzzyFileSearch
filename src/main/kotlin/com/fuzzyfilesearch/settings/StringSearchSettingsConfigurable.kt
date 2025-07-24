package com.fuzzyfilesearch.settings

import com.fuzzyfilesearch.settings.actionView.ActionViewWrapper
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class StringSearchSettingsConfigurable : Configurable {
    private lateinit var component: StringSearchSettingsComponent
    private var settings = GlobalSettings().getInstance()

    override fun createComponent(): JComponent? {
        component = StringSearchSettingsComponent(settings.state)
        component.keeper.components.forEach { component ->
            component.initialize()
        }

        return component.panel
    }

    override fun isModified(): Boolean {
        var modified = false
        component.keeper.components.forEach { component ->
            modified = modified || component.modified()
        }

        component.actionsCollectionPanel.components.forEach { component ->
            val settingsComp = component as? ActionViewWrapper ?: return@forEach
            val componentModified = settingsComp.isModified()
            modified = modified || componentModified
        }

        setWarningForDuplicateShortcuts(
            settings.state,
            component.actionsCollectionPanel.components,
            component.actionTypes)
        setWarningForDuplicateActionNames(
            settings.state,
            component.actionsCollectionPanel.components,
            component.actionTypes)
        modified = checkIfSizeIsModified(modified, component.actionsCollectionPanel.components.size)

        return modified
    }

    override fun apply() {
        component.keeper.components.forEach { component -> component.store() }

        clearSettingsAndClearActionRegistrationForTypes(settings.state, component.actionTypes)
        val newSettings = component.actionsCollectionPanel.components.map {
            (it as ActionViewWrapper).getStored()
        }
        addSettingsAndRegisterActions(settings.state, newSettings)
        component.refreshActionsPanel()
    }

    override fun getDisplayName(): String {
        return "FuzzyFileSearch Settings"
    }

    fun checkIfSizeIsModified(modified: Boolean, size: Int): Boolean {
        return modified || getActionSettingsForTypes(settings.state, component.actionTypes).size != size
    }
}