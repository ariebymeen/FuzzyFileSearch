package com.fuzzyfilesearch.settings

import javax.swing.KeyStroke

// Check if settings are correct. If not, return a string with the error message
public fun checkSettings(components: MutableList<SettingsComponent>): String? {
    val actionNameError = checkActionNames(components)
    if (actionNameError != null) {
        return actionNameError
    }

    val shortcutsError = checkShortcuts(components)
    if (shortcutsError != null) {
        return shortcutsError
    }

    return null
}

fun checkActionNames(components: MutableList<SettingsComponent>) : String? {
    val actionSet = mutableSetOf<String>()
    val names = mutableListOf<String>()
    components.forEach { comp ->
        val tableComponent = comp as? ActionsTableComponent ?: return@forEach
        names += tableComponent.table.getData().map{ action -> tableComponent.getActionName(action) }
    }

    for (name in names) {
        if (actionSet.contains(name)) {
            return "Action with name $name used multiple times. Action names must be unique"
        } else if (name.isEmpty()) {
            return "Trying to create action without name is not allowed"
        } else {
            actionSet += name
        }
    }

    return null
}

fun checkShortcuts(components: MutableList<SettingsComponent>) : String? {
    val actionSet = mutableSetOf<String>()
    val shortcuts = mutableListOf<String>()
    components.forEach { comp ->
        val tableComponent = comp as? ActionsTableComponent ?: return@forEach
        shortcuts += tableComponent.table.getData().map{ action -> tableComponent.getActionShortcut(action) }
    }

    for (shortcut in shortcuts) {
        if (shortcut.trim().isNotEmpty() && actionSet.contains(shortcut)) {
            return "Shortcut $shortcut used multiple times. Action names must be unique"
        } else {
            actionSet += shortcut
        }

        if (shortcut.trim().isNotEmpty()) {
            val keyStroke = KeyStroke.getKeyStroke(shortcut.trim())
            if (keyStroke == null) {
                return "Shortcut ${shortcut} is not valid"
            }
        }
    }

    return null
}
