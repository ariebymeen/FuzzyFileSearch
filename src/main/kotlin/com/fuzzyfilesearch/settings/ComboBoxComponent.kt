package com.fuzzyfilesearch.settings

import com.intellij.openapi.ui.ComboBox
import kotlin.reflect.KMutableProperty0

class ComboBoxComponent<E>(private var setting: KMutableProperty0<E>, private val values: Array<E>): SettingsComponent {
    val combobox = ComboBox<E>(values)
    override fun initialize() {
        combobox.selectedItem = setting.get()
    }

    override fun modified(): Boolean {
        return setting.get() != (combobox.selectedItem as E)
    }

    override fun store() {
        setting.set(combobox.selectedItem as E)
    }

}