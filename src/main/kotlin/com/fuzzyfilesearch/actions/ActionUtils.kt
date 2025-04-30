package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.actions.GrepInFiles.Companion.getActionPath
import com.fuzzyfilesearch.searchbox.getAllFilesInRoot
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.keymap.KeymapManager
import com.fuzzyfilesearch.settings.*
import com.fuzzyfilesearch.showErrorNotification
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.KeyStroke
import kotlin.io.path.Path

fun extractExtensions(extension: String): List<String> {
    if (extension.isNotEmpty()) {
        return extension.split('|', ',', ';', ':').map { ext -> ext.replace('.', ' ').trim().lowercase() }
    }
    return emptyList()
}

fun getActionName(name: String) : String {
    return "com.fuzzyfilesearch.$name"
}

fun registerAction(name: String, shortcut: String, action: AnAction) {
    val alreadyRegistered = ActionManager.getInstance().getAction(getActionName(name)) != null
    if (!alreadyRegistered) {
        println("Registering action: ${getActionName(name)}")
        ActionManager.getInstance().registerAction(getActionName(name), action)
    }

    if (shortcut.isNotEmpty()) {
        val keyStroke = KeyStroke.getKeyStroke(shortcut.trim())
        if (keyStroke == null) {
            showErrorNotification("Invalid shortcut" , "Shortcut $shortcut is not valid")
            return
        }
        val sc = KeyboardShortcut(keyStroke, null)
        KeymapManager.getInstance().activeKeymap.addShortcut(getActionName(name), sc)
    }
}

fun unregisterAction(name: String, shortcut: String) {
    println("Unregistering action: ${getActionName(name)}")
    ActionManager.getInstance().unregisterAction(getActionName(name))
    if (shortcut.isNotEmpty()) {
        val keyStroke = KeyStroke.getKeyStroke(shortcut.trim()) ?: return
        val sc = KeyboardShortcut(keyStroke, null)
        KeymapManager.getInstance().activeKeymap.removeShortcut(getActionName(name), sc)
    }
}

fun registerOpenRelativeFileActions(actions: Array<Array<String>>, settings: GlobalSettings.SettingsState) {
    actions.forEach { action ->
        run {
            registerAction(
                OpenRelativeFileAction.getActionName(action),
                OpenRelativeFileAction.getActionShortcut(action),
                OpenRelativeFileAction(action, settings.common.excludedDirs)
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

fun registerSearchAllFiles(actions: Array<Array<String>>, settings: GlobalSettings.SettingsState) {
    actions.forEach { action ->
        run {
            registerAction(action[0], action[2], SearchFileInPathAction(arrayOf(action[0], "/", action[1], action[2]), settings, true))
        }
    }
}

fun registerSearchFileMatchingPatternActions(actions: Array<Array<String>>, settings: GlobalSettings.SettingsState) {
    actions.forEach { action ->
        run {
            registerAction(
                SearchFilesWithPatternAction.getActionName(action),
                SearchFilesWithPatternAction.getActionShortcut(action),
                SearchFilesWithPatternAction(action, settings)
            )
        }
    }
}

fun registerSearchForRegexInFiles(actions: Array<Array<String>>, settings: GlobalSettings.SettingsState) {
    actions.forEach { action ->
        run {
            registerAction(
                RegexMatchInFiles.getActionName(action),
                RegexMatchInFiles.getActionShortcut(action),
                RegexMatchInFiles(action, settings)
            )
        }
    }
}

fun registerGrepInFilesActions(actions: Array<Array<String>>, settings: GlobalSettings.SettingsState) {
    actions.forEach { action ->
        run {
            registerAction(
                GrepInFiles.getActionName(action),
                GrepInFiles.getActionShortcut(action),
                GrepInFiles(action, settings)
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

fun getVirtualFileFromPath(filePath: String): VirtualFile? {
    val virtualFile = VfsUtil.findFile(Path(filePath), true)
    return virtualFile
}

fun getAllFilesInLocation(curFile: VirtualFile, project: Project, location: String, settings: GlobalSettings.SettingsState, extensions: List<String>): MutableList<VirtualFile> {
    val searchPath: String
    val files = mutableListOf<VirtualFile>()
    if (location.isEmpty() || (location[0] == '.' && location.length == 1)) {
        // Search only current file
        files.add(curFile)
    } else {
        if (location[0] == '/') { // Search from project root
            searchPath = project.basePath + location
        } else { // Search from current file
            searchPath = curFile.parent.path + "/" + location
        }
        val vfPath = getVirtualFileFromPath(searchPath) ?: return mutableListOf()
        val changeListManager = if (settings.common.searchOnlyFilesTrackedByVersionControl) ChangeListManager.getInstance(project) else null
        val allFiles = getAllFilesInRoot(vfPath, settings.common.excludedDirs, extensions, changeListManager)
        files.addAll(allFiles.map { file -> file.vf })
    }
    return files
}
