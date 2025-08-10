package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.actions.OpenRelativeFileAction.Companion.parseSettings
import com.fuzzyfilesearch.searchbox.getParentSatisfyingRegex
import com.fuzzyfilesearch.settings.GlobalSettings
import com.fuzzyfilesearch.showTimedNotification
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import kotlin.math.min

class OpenRelatedFileAction(var actionSettings: utils.ActionSettings,
                            var globalSettings: GlobalSettings.SettingsState) : AnAction(actionSettings.name)
{

    val settings = parseSettings(actionSettings.generic)
    // TODO: This action should do pattern matching, allowing the user to make groups of patterns like:
    //

    override fun actionPerformed(e: AnActionEvent) {}

    companion object {}
}