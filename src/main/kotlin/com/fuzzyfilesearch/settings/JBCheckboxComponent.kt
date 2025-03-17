package com.fuzzyfilesearch.settings

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import kotlin.reflect.KMutableProperty0

class JBCheckboxComponent(private var setting: KMutableProperty0<Boolean>): SettingsComponent {
    val checkbox = JBCheckBox()

    override fun initialize() {
        checkbox.isSelected = setting.get()
    }

    override fun modified(): Boolean {
        return checkbox.isSelected != setting.get()
    }

    override fun store() {
        setting.set(checkbox.isSelected)
    }
}