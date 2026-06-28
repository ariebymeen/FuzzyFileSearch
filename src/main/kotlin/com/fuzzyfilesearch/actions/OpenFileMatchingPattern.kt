package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.searchbox.getFileWithName
import com.fuzzyfilesearch.settings.GlobalSettings
import com.fuzzyfilesearch.showTimedNotification
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import kotlin.collections.map

data class OpenFileMatchingPatternSettings(val patterns: List<String>, val otherPatterns: List<String>)
data class OpenFileMatchingPatternAction(val name: String, val shortcut: String, val settings: OpenFileMatchingPatternSettings)

class OpenFileMatchingPattern(var actionSettings: OpenFileMatchingPatternAction,
                              var globalSettings: GlobalSettings.SettingsState) : AnAction(actionSettings.name)
{

    val settings = actionSettings.settings

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val currentFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE)
        if (currentFile == null) {
            showTimedNotification("${actionSettings.name} no open file", "Cannot perform action when no file is opened")
            return
        }

        // Resolve which pattern am I? This way I can know what %name% should be
        val matchingTokens = settings.otherPatterns.map { pattern ->
            getMatchingTokens(currentFile.name, pattern)
        }

        if (matchingTokens.max() == 0) {
            val currentFileMatch = settings.patterns.maxOfOrNull { pattern -> getMatchingTokens(currentFile.name, pattern) }
            if (currentFileMatch == 0 || currentFileMatch == null) {
                // Only show notification if current file is not already the target file
                showTimedNotification("${actionSettings.name} no match found", "Could not match any of the following patterns to the current file name: ${settings.otherPatterns.joinToString(",")}}")
            }
            return
        }

        val match = settings.otherPatterns[matchingTokens.indexOf(matchingTokens.filter { it > 0 }.min())]
        val start = match.split("%name%").take(1).getOrElse(0) { "" }
        val stop  = match.split("%name%").takeLast(1).getOrElse(0) { "" }
        val name = currentFile.name.substring(start.length, currentFile.name.length - stop.length)

        for (pattern in settings.patterns) {
            val fileNameToOpen = pattern.replace("%name%", name)

            val file = getFileWithName(project, currentFile.parent, fileNameToOpen)
            if (file != null) {
                FileEditorManager.getInstance(project).openFile(file, true)
                return
            }
        }

        // No file opened, so throw an error
        showTimedNotification("${actionSettings.name} no file not found", "Could not find file matching pattern for %name% = $name")
    }

    private fun getMatchingTokens(filename: String, pattern: String): Int {
        val start = pattern.split("%name%").take(1).getOrElse(0) { "" }
        val stop  = pattern.split("%name%").takeLast(1).getOrElse(0) { "" }
        println("Pattern $pattern splits into $start, $stop")

        if (filename.startsWith(start) && filename.endsWith(stop)) {
            return start.length + stop.length
        }
        return 0
    }

    companion object {
        fun getListOfPatterns(actionSettings: List<String>): List<String> {
            val patterns = mutableListOf<String>()
            val nofActions = actionSettings.size / 3
            for (i in 0 until nofActions)  {
                patterns.add(actionSettings[i * 3 + 1])
            }
            return patterns
        }

        fun parseSettings(actionSettings: List<String>): List<OpenFileMatchingPatternAction> {
            val actions = mutableListOf<OpenFileMatchingPatternAction>()
            val nofActions = actionSettings.size / 3

            val allPatterns = getListOfPatterns(actionSettings)
            for (i in 0 until nofActions)  {
                val pattern  = actionSettings[i * 3 + 1]
                val patterns = pattern.split('|', ',', ';', ':').map { it.trim() }

                val action = OpenFileMatchingPatternAction(
                    name      = actionSettings[i * 3 + 0],
                    shortcut  = actionSettings[i * 3 + 2],
                    settings  = OpenFileMatchingPatternSettings(
                        patterns = patterns,
                        otherPatterns = allPatterns.filter { pattern -> pattern != actionSettings[i * 3 + 1] }.flatMap { pattern -> pattern.split('|', ',', ';', ':').map { it.trim() } },))
                actions.add(action)
            }

            return actions
        }

        fun register(settings: utils.ActionSettings, globalSettings: GlobalSettings.SettingsState) {
            val actionSettings = parseSettings(settings.generic)
            actionSettings.forEach { settings ->
                val action = OpenFileMatchingPattern(settings, globalSettings)
                utils.registerAction(settings.name, settings.shortcut, action)
            }
        }

        fun unregister(settings: utils.ActionSettings) {
            val actionSettings = parseSettings(settings.generic)
            actionSettings.forEach { settings ->
                utils.unregisterAction(settings.name, settings.shortcut)
            }

        }
    }
}