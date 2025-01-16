package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.services.PopupMediator
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

enum class ShortcutType {
    OPEN_FILE_IN_HORIZONTAL_SPLIT,
    OPEN_FILE_IN_VERTICAL_SPLIT,
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

class ShortcutAction(val actionName: String = "OpenInVerticalSplit",
                     val actionType: ShortcutType = ShortcutType.OPEN_FILE_IN_VERTICAL_SPLIT,
                     val shortcut: String = "") : AnAction(actionName) {
    override fun actionPerformed(e: AnActionEvent) {
        println("Shortcut action performed")
        val instance = e.project?.service<PopupMediator>()?.getPopupInstance()
        instance?.handleActionShortcut(actionType)
    }

    companion object {
        fun fromArray(settings: Array<String>): ShortcutAction {
            val shA = ShortcutType.fromIndex(settings[1].toInt())
            return ShortcutAction(settings[0], shA!!, settings[2])
        }

        fun toArray(action: ShortcutAction): Array<String> {
            return arrayOf(action.actionName, ShortcutType.toIndex(action.actionType).toString(), action.shortcut)
        }
    }
}
