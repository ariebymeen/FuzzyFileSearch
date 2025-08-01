package com.fuzzyfilesearch.settings.actionView

import com.fuzzyfilesearch.actions.utils
import com.fuzzyfilesearch.components.LabeledTextField
import javax.swing.JPanel

abstract class ActionViewBase() {
    // Keep track of your own stored set as the mSettings array also contains elements not of your set
    var initialSettings: Array<String> = emptyArray()
    val actionNameField =
            LabeledTextField("Name: ", "Enter a name (no spaces) which is used to register the action", "ActionName")
    val shortcutField = LabeledTextField("Shortcut: ", "Enter a shortcut (example: alt shift U)")

    /** Add interface items to panel */
    abstract fun addToPanel(panel: JPanel)

    /** Add interface items to panel, but from the settings */
    abstract fun initialize(settings: utils.ActionSettings)
    abstract fun modified(): Boolean
    abstract fun verify(): String

    /** Save the current state into the storedSet object */
    abstract fun getStored(): Array<String>

    /** Return the help string */
    abstract fun help(): String

    fun getActionName(): String {
        return actionNameField.text()
    }

    fun getShortcut(): String {
        return shortcutField.text()
    }
}