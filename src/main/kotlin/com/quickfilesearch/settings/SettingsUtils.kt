package com.quickfilesearch.settings

import com.quickfilesearch.actions.QuickFileSearchAction
import com.quickfilesearch.actions.SearchFileInPathAction
import com.quickfilesearch.actions.SearchRelativeFileAction

// Check if settings are correct. If not, return a string with the error message
// TODO: Currently checks only one of the settings, should always check all settings
fun checkSettings(globalSettings: GlobalSettingsComponent?, projectSettings: ProjectSettingsComponent?): String? {
    val actionNameError = checkActionNames(globalSettings, projectSettings);
    if (actionNameError != null) {
        return actionNameError
    }

    val shortcutsError = checkShortcuts(globalSettings, projectSettings);
    if (shortcutsError != null) {
        return shortcutsError
    }

    return null
}

fun checkActionNames(globalSettings: GlobalSettingsComponent?, projectSettings: ProjectSettingsComponent?) : String? {
    val actionSet = mutableSetOf<String>()
    var names = emptyList<String>()
    if (globalSettings != null) {
        names = globalSettings.openRelativeFileActionsTable.getData().map{ action -> QuickFileSearchAction.getActionName(action) }
        names += globalSettings.searchRelativeFileActionsTable.getData().map{ action -> SearchRelativeFileAction.getActionName(action) }
        names += globalSettings.searchPathActionsTable.getData().map{ action -> SearchFileInPathAction.getActionName(action) }
    }
    if (projectSettings!= null) {
        names = projectSettings.openRelativeFileActionsTable.getData().map{ action -> QuickFileSearchAction.getActionName(action) }
        names += projectSettings.searchRelativeFileActionsTable.getData().map{ action -> SearchRelativeFileAction.getActionName(action) }
        names += projectSettings.searchPathActionsTable.getData().map{ action -> SearchFileInPathAction.getActionName(action) }
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

fun checkShortcuts(globalSettings: GlobalSettingsComponent?, projectSettings: ProjectSettingsComponent?) : String? {
    val actionSet = mutableSetOf<String>()
    var shortcuts = emptyList<String>()
    if (globalSettings != null) {
        shortcuts = globalSettings.openRelativeFileActionsTable.getData().map{ action -> QuickFileSearchAction.getActionShortcut(action) }
        shortcuts += globalSettings.searchRelativeFileActionsTable.getData().map{ action -> SearchRelativeFileAction.getActionShortcut(action) }
        shortcuts += globalSettings.searchPathActionsTable.getData().map{ action -> SearchFileInPathAction.getActionShortcut(action) }
    }
    if (projectSettings != null) {
        shortcuts = projectSettings.openRelativeFileActionsTable.getData().map{ action -> QuickFileSearchAction.getActionShortcut(action) }
        shortcuts += projectSettings.searchRelativeFileActionsTable.getData().map{ action -> SearchRelativeFileAction.getActionShortcut(action) }
        shortcuts += projectSettings.searchPathActionsTable.getData().map{ action -> SearchFileInPathAction.getActionShortcut(action) }
    }

    for (shortcut in shortcuts) {
        if (shortcut.isNotEmpty() && actionSet.contains(shortcut)) {
            return "Shortcut $shortcut used multiple times. Action names must be unique"
        } else {
            actionSet += shortcut
        }
    }

    return null
}
