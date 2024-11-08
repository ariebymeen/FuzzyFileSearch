package com.fuzzyfilesearch.settings

import com.fuzzyfilesearch.actions.QuickFileSearchAction
import com.fuzzyfilesearch.actions.SearchFileInPathAction
import com.fuzzyfilesearch.actions.SearchOpenFilesAction
import com.fuzzyfilesearch.actions.SearchRelativeFileAction

// Check if settings are correct. If not, return a string with the error message
fun checkSettings(globalSettings: GlobalSettingsComponent): String? {
    val actionNameError = checkActionNames(globalSettings);
    if (actionNameError != null) {
        return actionNameError
    }

    val shortcutsError = checkShortcuts(globalSettings);
    if (shortcutsError != null) {
        return shortcutsError
    }

    return null
}

fun checkActionNames(globalSettings: GlobalSettingsComponent) : String? {
    val actionSet = mutableSetOf<String>()
    val names = mutableListOf<String>()
    names += globalSettings.openRelativeFileActionsTable.getData().map{ action -> QuickFileSearchAction.getActionName(action) }
    names += globalSettings.searchRelativeFileActionsTable.getData().map{ action -> SearchRelativeFileAction.getActionName(action) }
    names += globalSettings.searchPathActionsTable.getData().map{ action -> SearchFileInPathAction.getActionName(action) }
    names += globalSettings.searchRecentFiles.getData().map{ action -> SearchFileInPathAction.getActionName(action) }
    names += globalSettings.searchOpenFiles.getData().map{ action -> SearchOpenFilesAction.getActionName(action) }

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

fun checkShortcuts(globalSettings: GlobalSettingsComponent) : String? {
    val actionSet = mutableSetOf<String>()
    val shortcuts = mutableListOf<String>()
    shortcuts += globalSettings.openRelativeFileActionsTable.getData().map{ action -> QuickFileSearchAction.getActionShortcut(action) }
    shortcuts += globalSettings.searchRelativeFileActionsTable.getData().map{ action -> SearchRelativeFileAction.getActionShortcut(action) }
    shortcuts += globalSettings.searchPathActionsTable.getData().map{ action -> SearchFileInPathAction.getActionShortcut(action) }
    shortcuts += globalSettings.searchRecentFiles.getData().map{ action -> SearchFileInPathAction.getActionShortcut(action) }
    shortcuts += globalSettings.searchOpenFiles.getData().map{ action -> SearchOpenFilesAction.getActionShortcut(action) }

    for (shortcut in shortcuts) {
        if (shortcut.isNotEmpty() && actionSet.contains(shortcut)) {
            return "Shortcut $shortcut used multiple times. Action names must be unique"
        } else {
            actionSet += shortcut
        }
    }

    return null
}
