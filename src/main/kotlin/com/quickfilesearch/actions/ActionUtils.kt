package com.quickfilesearch.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.keymap.KeymapManager
import com.quickfilesearch.settings.*
import javax.swing.KeyStroke

fun sanitizeExtension(extension: String): String {
    if (extension.isNotEmpty() && extension[0] == '.') {
        return extension.substring(1)
    }
    return extension
}

fun registerAction(name: String, shortcut: String, action: AnAction) {
    println("Registering action: $name")
    val alreadyRegistered = ActionManager.getInstance().getAction(name) != null
    if (!alreadyRegistered) ActionManager.getInstance().registerAction(name, action)

    if (shortcut.isNotEmpty()) {
        val shortcut = KeyboardShortcut(KeyStroke.getKeyStroke(shortcut), null)
        KeymapManager.getInstance().activeKeymap.addShortcut(name, shortcut);
    }
}

fun unregisterAction(name: String, shortcut: String) {
    println("Unregistering action: $name")
    ActionManager.getInstance().unregisterAction(name)
    if (shortcut.isNotEmpty()) {
        val shortcut = KeyboardShortcut(KeyStroke.getKeyStroke(shortcut), null)
        KeymapManager.getInstance().activeKeymap.removeShortcut(name, shortcut);
    }
}

fun registerQuickFileSearchActions(actions: Array<Array<String>>, settings: GlobalSettings.SettingsState) {
    actions.forEach { action ->
        run {
            registerAction(
                QuickFileSearchAction.getActionName(action),
                QuickFileSearchAction.getActionShortcut(action),
                QuickFileSearchAction(action, settings.excludedDirs)
            )
        }
    }
}

fun registerSearchRelativeFileActions(actions: Array<Array<String>>, settings: GlobalSettings.SettingsState) {
    actions.forEach { action ->
        run {
            registerAction(
                SearchRelativeFileAction.getActionName(action),
                SearchRelativeFileAction.getActionShortcut(action),
                SearchRelativeFileAction(action, settings)
            )
        }
    }
}

fun registerSearchFileInPathActions(actions: Array<Array<String>>, settings: GlobalSettings.SettingsState) {
    actions.forEach { action ->
        run {
            registerAction(
                SearchFileInPathAction.getActionName(action),
                SearchFileInPathAction.getActionShortcut(action),
                SearchFileInPathAction(action, settings)
            )
        }
    }
}

fun registerSearchRecentFiles(actions: Array<Array<String>>, settings: GlobalSettings.SettingsState) {
    actions.forEach { action ->
        run {
            println("register recent files with sc ${SearchRecentFilesAction.getActionShortcut(action)}")
            registerAction(
                SearchRecentFilesAction.getActionName(action),
                SearchRecentFilesAction.getActionShortcut(action),
                SearchRecentFilesAction(action, settings)
            )
        }
    }
}

fun registerSearchOpenFiles(actions: Array<Array<String>>, settings: GlobalSettings.SettingsState) {
    actions.forEach { action ->
        run {
            registerAction(
                SearchOpenFilesAction.getActionName(action),
                SearchOpenFilesAction.getActionShortcut(action),
                SearchOpenFilesAction(action, settings)
            )
        }
    }
}

fun unregisterActions(actions: Array<Array<String>>, getName: (Array<String>) -> String, getShortcut: (Array<String>) -> String ) {
    actions.map{ action -> unregisterAction(getName(action), getShortcut(action)) }
}

fun isEqual(lhs: Array<Array<String>>, rhs: Array<Array<String>>) : Boolean {
    if (lhs.size != rhs.size) return false

    for (row in 0..lhs.size - 1) {
        if (lhs[row].size != rhs[row].size) return false
        for (col in 0..lhs[row].size - 1) {
            if (lhs[row][col] != rhs[row][col]) return false
        }
    }

    return true
}

fun printAction(actions: Array<Array<String>>) {
    actions.forEach { subArray ->
        println(subArray.joinToString(" - "))
    }
}