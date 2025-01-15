package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.services.PopupMediator
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

class TabKeyAction : AnAction("FuzzyFileSearchTabKeyAction") {
    override fun actionPerformed(e: AnActionEvent) {
        println("TabKeyAction.actionPerformed")
        val instance = e.project?.service<PopupMediator>()?.getPopupInstance()
        println("Instance: $instance")
        instance?.handleActionShortcut(ShortcutType.TAB_PRESSED)
    }
}