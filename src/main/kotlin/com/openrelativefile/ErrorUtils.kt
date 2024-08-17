package com.openrelativefile

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import java.util.*
import javax.swing.SwingUtilities


fun showErrorNotification(title: String, content: String) {
    val notification = Notification("OpenRelativeFileGroup", title, content, NotificationType.ERROR)
    Notifications.Bus.notify(notification)
}

fun showTimedNotification(title: String, content: String) {
    val notification = Notification("OpenRelativeFileGroup", title, content, NotificationType.INFORMATION)
    Notifications.Bus.notify(notification)

    // Schedule a task to close the notification after the specified duration
    Timer().schedule(object : TimerTask() {
        override fun run() {
            SwingUtilities.invokeLater(java.lang.Runnable { notification.expire() })
        }
    }, 5000)
}