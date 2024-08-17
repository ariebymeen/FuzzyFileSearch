package com.openrelativefile.Actions

import com.openrelativefile.getParentSatisfyingRegex
import com.openrelativefile.showTimedNotification
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.LocalFileSystem

class OpenRelativeFileAction(var action: Array<String>,
                             var excludedDirs: Set<String>,
                             var maxDistance: Int) : AnAction(getActionName(action))
{
    val regex = Regex(pattern = action[1], options = setOf(RegexOption.IGNORE_CASE))

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val currentFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE)
        if (currentFile  == null) {
            showTimedNotification("${action[0]} No open file", "Cannot perform action when no file is opened");
            return
        }

        val matchingFile = getParentSatisfyingRegex(project, currentFile, regex, excludedDirs, 0, maxDistance)
        if (matchingFile == null) {
            showTimedNotification("${action[0]} Could not find file", "Could not find file satisfying regex ${regex.pattern}");
            return
        }

        val fileName = action[2].replace("%name%", matchingFile.nameWithoutExtension);
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(matchingFile.parent.path + '/' + fileName)
        if (virtualFile == null) {
            showTimedNotification("${action[0]} File not found", "Trying to open file ${matchingFile.parent.path + '/' + fileName}");
            return
        }

        FileEditorManager.getInstance(project).openFile(virtualFile, true)
    }

    companion object {
        fun getActionName(actionSettins: Array<String>) : String {
            return actionSettins[0]
        }
        fun getActionShortcut(actionSettins: Array<String>) : String {
            return actionSettins[3]
        }
    }
}