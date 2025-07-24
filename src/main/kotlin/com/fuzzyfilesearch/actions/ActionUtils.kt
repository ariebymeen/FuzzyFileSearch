package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.searchbox.getAllFilesInRoot
import com.fuzzyfilesearch.settings.GlobalSettings
import com.fuzzyfilesearch.showErrorNotification
import com.fuzzyfilesearch.showTimedNotification
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.util.regex.PatternSyntaxException
import javax.swing.KeyStroke
import kotlin.io.path.Path

object utils {
    fun extractExtensions(extension: String): List<String> {
        if (extension.isNotEmpty()) {
            val result = extension.split('|', ',', ';', ':').map { ext -> ext.replace('.', ' ').trim().lowercase() }
            return result.filter { it.trim().isNotEmpty() }
        }
        return emptyList()
    }

    fun getActionName(name: String): String {
        return "com.fuzzyfilesearch.$name"
    }

    /** All actions are stored as Array<String> as this serializes easily.
     *  All actions are stored as [type, name, shortcut, <<Custom action settings>>]
     */
    data class ActionSettings(
        val type: ActionType,
        val name: String,
        val shortcut: String,
        val generic: List<String>)

    fun getGenericActionSettings(action: Array<String>): ActionSettings {
        try {
            val type = ActionType.valueOf(action[0]) // TODO: Catch?
            val name = action[1]
            val shortcut = action.getOrElse(2) { "" }
            val settings = action.takeLast(action.size - 3)
            return ActionSettings(type, name, shortcut, settings)
        } catch (e: IllegalArgumentException) {
            return ActionSettings(ActionType.SEARCH_FILE_IN_PATH, "Invalid", "", emptyList())
        }
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
                showErrorNotification(
                    "Invalid shortcut",
                    "Shortcut $shortcut is not valid. (Used for FuzzyFileSearch $name)")
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

    fun registerActionsFromSettings(actions: Array<Array<String>>, settings: GlobalSettings.SettingsState) {
        actions.forEach { action ->
            try {
                val type = ActionType.valueOf(action[0])
                val action = getGenericActionSettings(action)
                when (type) {
                    ActionType.SEARCH_FILE_IN_PATH          -> SearchFileInPathAction.register(action, settings)
                    ActionType.SEARCH_RECENT_FILES          -> SearchRecentFilesAction.register(action, settings)
                    ActionType.SEARCH_OPEN_FILES            -> SearchOpenFilesAction.register(action, settings)
                    ActionType.SEARCH_FILE_IN_RELATED_PATH  -> SearchRelativeFileAction.register(action, settings)
                    ActionType.OPEN_RELATIVE_FILE           -> OpenRelativeFileAction.register(action, settings)
                    ActionType.SEARCH_FILE_MATCHING_PATTERN -> SearchFilesWithPatternAction.register(action, settings)
                    ActionType.REGEX_SEARCH_IN_FILES        -> RegexMatchInFiles.register(action, settings)
                    ActionType.GREP_IN_FILES                -> GrepInFiles.register(action, settings)
                }
            } catch (e: IllegalArgumentException) {
                showErrorNotification("Error parsing setting", e.message.orEmpty())
            }
        }
    }

    fun unregisterActions(
        actions: Array<Array<String>>,
        getName: (Array<String>) -> String,
        getShortcut: (Array<String>) -> String
                         ) {
        actions.map { action -> unregisterAction(getName(action), getShortcut(action)) }
    }

    fun isEqual(lhs: Array<Array<String>>, rhs: Array<Array<String>>): Boolean {
        if (lhs.size != rhs.size) return false

        for (row in 0..lhs.size - 1) {
            if (lhs[row].size != rhs[row].size) return false
            for (col in 0..lhs[row].size - 1) {
                if (lhs[row][col] != rhs[row][col]) return false
            }
        }

        return true
    }

    fun isEqual(lhs: Array<String>, rhs: Array<String>): Boolean {
        if (lhs.size != rhs.size) return false

        for (idx in 0..lhs.size - 1) {
            if (lhs[idx] != rhs[idx]) return false
        }

        return true
    }

    fun getVirtualFileFromPath(filePath: String): VirtualFile? {
        val virtualFile = VfsUtil.findFile(Path(filePath), true)
        if (virtualFile == null) {
            showTimedNotification(
                "${filePath} path not found",
                "Trying to open path ${filePath}, but this path does not exist"
                                 )
        }
        return virtualFile
    }

    fun getAllFilesInLocation(
        curFile: VirtualFile,
        project: Project,
        location: String,
        settings: GlobalSettings.SettingsState,
        extensions: List<String>,
        onlyVcsTrackedFiles: Boolean): MutableList<VirtualFile> {
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
            val changeListManager = if (onlyVcsTrackedFiles) ChangeListManager.getInstance(project) else null
            val allFiles = getAllFilesInRoot(vfPath, settings.common.excludedDirs, extensions, changeListManager)
            files.addAll(allFiles.map { file -> file.vf })
        }
        return files
    }

    fun getLineNumberFromVirtualFile(vf: VirtualFile, offset: Int, printErr: Boolean): Int? {
        var lineNr: Int? = 0
        runReadAction {
            try {
                val document = FileDocumentManager.getInstance().getDocument(vf)
                lineNr = document?.getLineNumber(offset)?.plus(1)
            } catch (e: IndexOutOfBoundsException) {
                if (printErr) {
                    println("Line number $offset could not be retrieved for file ${vf.name}: ${e.message}")
                }
            }
        }
        return lineNr
    }

    fun getAllOpenFiles(project: Project): List<VirtualFile> {
        val fileEditorManager = FileEditorManager.getInstance(project)
        return fileEditorManager.openFiles.toList()
    }

    fun parseRegex(pattern: String): Regex {
        try {
            return Regex(
                pattern = pattern,
                options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        } catch (e: PatternSyntaxException) {
            showErrorNotification("Invalid regex", e.message!!)
            return Regex("")
        }
    }
}