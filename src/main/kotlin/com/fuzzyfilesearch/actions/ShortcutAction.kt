package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.services.PopupMediator
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

enum class ShortcutType {
    OPEN_FILE_IN_HORIZONTAL_SPLIT,
    OPEN_FILE_IN_VERTICAL_SPLIT,
    TAB_PRESSED
}

class ShortcutAction(val actionName: String,
                     val actionType: ShortcutType) : AnAction(actionName) {
    override fun actionPerformed(e: AnActionEvent) {
        val instance = e.project?.service<PopupMediator>()?.getPopupInstance()
        instance?.handleActionShortcut(actionType)
    }

}
