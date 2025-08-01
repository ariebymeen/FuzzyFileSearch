package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.searchbox.getParentSatisfyingRegex
import com.fuzzyfilesearch.settings.GlobalSettings
import com.fuzzyfilesearch.showTimedNotification
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile

class OpenRelativeFileAction(
    var actionSettings: utils.ActionSettings,
    var excludedDirs: Set<String>) : AnAction(actionSettings.name) {
    data class Settings(val regex: Regex, val filePatterns: List<String>)

    val settings = parseSettings(actionSettings.generic)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val currentFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE)
        if (currentFile == null) {
            showTimedNotification("${actionSettings.name} no open file", "Cannot perform action when no file is opened")
            return
        }

        val matchingFile = getParentSatisfyingRegex(project, currentFile, settings.regex, excludedDirs)
        if (matchingFile == null) {
            showTimedNotification(
                "${actionSettings.name} Could not find file",
                "Could not find file satisfying regex ${settings.regex.pattern}")
            return
        }

        var fileFound = false
        for (rawFileName in settings.filePatterns) {
            var virtualFile: VirtualFile? = null
            if (rawFileName.contains("%rname%")) {
                virtualFile = getReferenceFileName(rawFileName, matchingFile)
            } else {
                val matchingPath = matchingFile.parent.path + "/" + rawFileName.split('/').dropLast(1).joinToString("/")
                val matchingPattern = rawFileName.split("/").last()

                val directory = LocalFileSystem.getInstance().findFileByPath(matchingPath)
                if (directory != null) {
                    virtualFile = getMatchingCnameFile(directory, currentFile, matchingPattern)
                }
            }

            if (virtualFile != null) {
                fileFound = true
                FileEditorManager.getInstance(project).openFile(virtualFile, true)
                break
            }
        }

        if (!fileFound) {
            showTimedNotification(
                "${actionSettings.name} File not found",
                "Trying to open file in path ${matchingFile.parent.path + '/'} with pattern ${settings.filePatterns.joinToString(" | ")}")
        }
    }

    fun getReferenceFileName(rawFileName: String, matchingFile: VirtualFile): VirtualFile? {
        val fileName = rawFileName.replace("%rname%", matchingFile.nameWithoutExtension)
        return LocalFileSystem.getInstance().findFileByPath(matchingFile.parent.path + '/' + fileName)
    }

    fun getMatchingCnameFile(searchDirectory: VirtualFile, currentFile: VirtualFile, pattern: String): VirtualFile? {
        val currentFileName = currentFile.nameWithoutExtension

        // Evaluate all files in directory, if there is a match with the pattern, return this one
        searchDirectory.children!!
            .filter { vf -> vf.isFile }
            .forEach { vf ->
                val patternMatch = pattern.replace("%cname%", currentFileName)
                if (vf.name.contains(patternMatch)) {
                    return vf
                }
            }

        for (child in searchDirectory.children!!.filter { vf -> vf.isDirectory }) {
            val file = getMatchingCnameFile(child, currentFile, pattern)
            if (file != null) {
                return file
            }
        }

        return null
    }

    companion object {
        fun parseSettings(actionSettings: List<String>): Settings {
            return Settings(
                regex = utils.parseRegex(actionSettings[0]),
                filePatterns = actionSettings[1].split('|').map { path -> path.trim() })
        }

        fun register(settings: utils.ActionSettings, globalSettings: GlobalSettings.SettingsState) {
            val action = OpenRelativeFileAction(settings, globalSettings.common.excludedDirs)
            utils.registerAction(settings.name, settings.shortcut, action)
        }

    }
}