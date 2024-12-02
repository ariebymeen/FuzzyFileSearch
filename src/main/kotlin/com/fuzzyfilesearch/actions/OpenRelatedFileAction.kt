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

class OpenRelatedFileAction(var action: Array<String>,
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

        // TODO: Look through all files?
        // | -- MyTestFileTest.cpp
        // | -- MyTestFileTest.h
        // | -- MyTestFile.h
        // | -- MyTestFile.cpp

//        val fileNames = getOpenFilesPaths(action)
//        for (rawFileName in fileNames) {
//            var virtualFile: VirtualFile? = null
//            if (rawFileName.contains("%rname%")) {
//                virtualFile = getReferenceFileName(rawFileName, matchingFile)
//            } else {
//                val matchingPath    = matchingFile.parent.path + "/" + rawFileName.split('/').dropLast(1).joinToString("/")
//                val matchingPattern = rawFileName.split("/").last()
//                println("File path: ${matchingFile.parent.path}, subpath: ${rawFileName.split('/').dropLast(1).joinToString("/")}, total: $matchingPath")
//
//                val directory = LocalFileSystem.getInstance().findFileByPath(matchingPath)
//                if (directory != null) {
//                    virtualFile = getMatchingCnameFile(directory, currentFile, matchingPattern)
//                }
//            }
//
//            if (virtualFile != null) {
//                fileFound = true
//                FileEditorManager.getInstance(project).openFile(virtualFile, true)
//                break
//            }
//        }
//
//        if (!fileFound) {
//            showTimedNotification("${action[0]} File not found", "Trying to open file in path ${matchingFile.parent.path + '/'} with pattern $fileNames")
//        }
    }

    companion object {
        fun getActionName(actionSettings: Array<String>) : String {
            return actionSettings[0]
        }
        fun getActionPattern(actionSettings: Array<String>) : String {
            return actionSettings[0]
        }
        fun getActionShortcut(actionSettings: Array<String>) : String {
            return actionSettings[3]
        }
    }
}