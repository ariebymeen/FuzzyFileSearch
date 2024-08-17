package com.openrelativefile.Actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.keymap.KeymapManager
import com.openrelativefile.Settings.*
import javax.swing.KeyStroke

fun registerAction(name: String, shortcut: String, action: AnAction) {
    println("Registering action: $name")
    ActionManager.getInstance().registerAction(name, action)

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

fun registerOpenRelativeFileActions(actions: Array<Array<String>>, settings: GlobalSettings.SettingsState) {
    actions.forEach { action ->
        run {
            registerAction(
                OpenRelativeFileAction.getActionName(action),
                OpenRelativeFileAction.getActionShortcut(action),
                OpenRelativeFileAction(action, settings.excludedDirs, settings.distanceSearchMaxFileDistance)
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