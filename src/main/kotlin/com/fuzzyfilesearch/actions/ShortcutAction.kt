package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.services.PopupMediator
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

enum class ShortcutType {
    OPEN_FILE_IN_HORIZONTAL_SPLIT,
    OPEN_FILE_IN_VERTICAL_SPLIT,
    OPEN_FILE_IN_ACTIVE_EDITOR,
    TAB_PRESSED;

    companion object {
        fun fromIndex(index: Int): ShortcutType? {
            return ShortcutType.values().getOrNull(index) // Safely get the enum by index
        }
        fun toIndex(type: ShortcutType): Int {
            return type.ordinal
        }
    }
}

/* very simple action class that is attached to the open popup window to enable the use of custom shortcuts
   in the popup menu, like ctrl H to open in split view, etc...
 */
class ShortcutAction(val actionName: String = "OpenInVerticalSplit",
                     val actionType: ShortcutType = ShortcutType.OPEN_FILE_IN_VERTICAL_SPLIT,
                     val shortcut: String = "") : AnAction(actionName) {
    override fun actionPerformed(e: AnActionEvent) {
        val instance = e.project?.service<PopupMediator>()?.getPopupInstance()
        instance?.handleActionShortcut(actionType)
    }
}
