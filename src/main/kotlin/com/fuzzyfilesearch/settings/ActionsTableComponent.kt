package com.fuzzyfilesearch.settings

import com.fuzzyfilesearch.actions.OpenRelativeFileAction
import com.fuzzyfilesearch.actions.isEqual
import com.fuzzyfilesearch.actions.registerOpenRelativeFileActions
import com.fuzzyfilesearch.actions.unregisterActions
import kotlin.reflect.KMutableProperty0

class ActionsTableComponent(private var setting: KMutableProperty0<Array<Array<String>>>,
                            header: Array<String>,
                            default: Array<String>,
                            private val nameIndex: Int,
                            private val shortcutIndex: Int,
                            private val createActionCb: (Array<Array<String>>, GlobalSettings.SettingsState) -> Unit,
                            private val settings: GlobalSettings.SettingsState): SettingsComponent {
    val table = ActionsTable(header, default)

    override fun initialize() {
        table.setData(setting.get())
    }

    override fun modified(): Boolean {
        println("Setting: ${setting.get()}, data: ${table.getData()}")
        return !isEqual(setting.get(), table.getData())
    }

    override fun store() {
        if (modified()) {
            unregisterActions(setting.get(), ::getActionName, ::getActionShortcut)
            setting.set(table.getData())
            createActionCb(setting.get(), settings)
        }
    }

    fun getActionName(settings: Array<String>): String {
        return settings[nameIndex]
    }

    fun getActionShortcut(settings: Array<String>): String {
        return settings[shortcutIndex]
    }
}