package com.fuzzyfilesearch.services

import com.fuzzyfilesearch.actions.ShortcutType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.event.KeyEvent

@Service(Service.Level.PROJECT)
class TabKeyPostProcessor(private val mProject: Project) {
    fun registerProcessor() {
        Toolkit.getDefaultToolkit().addAWTEventListener(
            {event: AWTEvent? -> if (event is KeyEvent) {
                val keyEvent = event
                if (keyEvent.id == KeyEvent.KEY_PRESSED && keyEvent.keyCode == KeyEvent.VK_TAB) {
                    val instance = mProject.service<PopupMediator>().getPopupInstance()
                    if (instance?.mPopup != null && !instance.mPopup!!.isDisposed) {
                        instance.handleActionShortcut(ShortcutType.TAB_PRESSED)
                        keyEvent.consume() // Prevent default behavior
                    }
                }
            }
            },
            AWTEvent.KEY_EVENT_MASK
        )
    }
}