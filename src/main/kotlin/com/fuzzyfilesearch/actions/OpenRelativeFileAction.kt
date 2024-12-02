package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.searchbox.getParentSatisfyingRegex
import com.fuzzyfilesearch.showTimedNotification
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import kotlin.math.min

class OpenRelativeFileAction(var action: Array<String>,
                             var excludedDirs: Set<String>) : AnAction(getActionName(action))
{
    val regex = Regex(pattern = action[1], options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val currentFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE)
        if (currentFile == null) {
            showTimedNotification("${action[0]} No open file", "Cannot perform action when no file is opened")
            return
        }

        val matchingFile: VirtualFile?
        if (action[1].isEmpty()) {
            matchingFile = currentFile
        } else {
            matchingFile = getParentSatisfyingRegex(project, currentFile, regex, excludedDirs)
        }

        if (matchingFile == null) {
            showTimedNotification("${action[0]} Could not find file", "Could not find file satisfying regex ${regex.pattern}")
            return
        }

        var fileFound = false
        val fileNames = getOpenFilesPaths(action)
        for (rawFileName in fileNames) {
            var virtualFile: VirtualFile? = null
            if (rawFileName.contains("%rname%")) {
                virtualFile = getReferenceFileName(rawFileName, matchingFile)
            } else {
                val matchingPath    = matchingFile.parent.path + "/" + rawFileName.split('/').dropLast(1).joinToString("/")
                val matchingPattern = rawFileName.split("/").last()
//                println("File path: ${matchingFile.parent.path}, subpath: ${rawFileName.split('/').dropLast(1).joinToString("/")}, total: $matchingPath")

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
            showTimedNotification("${action[0]} File not found", "Trying to open file in path ${matchingFile.parent.path + '/'} with pattern $fileNames")
        }
    }

    fun getReferenceFileName(rawFileName: String, matchingFile: VirtualFile): VirtualFile? {
        val fileName = rawFileName.replace("%rname%", matchingFile.nameWithoutExtension)
        return LocalFileSystem.getInstance().findFileByPath(matchingFile.parent.path + '/' + fileName)
    }

    fun getMatchingCnameFile(searchDirectory: VirtualFile, currentFile: VirtualFile, pattern: String) : VirtualFile? {
        val currentFileName = currentFile.nameWithoutExtension

        // Evaluate all files in directory, if there is a match with the pattern, return this one
        searchDirectory.children!!
            .filter { vf -> vf.isFile }
            .forEach { vf ->
                val strLen = min(vf.nameWithoutExtension.length, currentFileName.length)
                val patternMatch = pattern.replace("%cname%", vf.nameWithoutExtension.substring(0, strLen))
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
        fun getActionName(actionSettings: Array<String>) : String {
            return actionSettings[0]
        }
        fun getOpenFilesPaths(actionSettings: Array<String>) : List<String> {
            return actionSettings[2].split('|').map { path -> path.trim() }
        }
        fun getActionShortcut(actionSettings: Array<String>) : String {
            return actionSettings[3]
        }
    }
}