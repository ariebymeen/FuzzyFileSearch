package com.fuzzyfilesearch.settings

import com.intellij.ui.components.JBTextField
import kotlin.reflect.KMutableProperty0

class JBTextFieldComponent(private var setting: KMutableProperty0<String>): SettingsComponent {
    val textfield = JBTextField()

    override fun initialize() {
        textfield.text = setting.get()
    }

    override fun modified(): Boolean {
        return textfield.text != setting.get()
    }

    override fun store() {
        setting.set(textfield.text)
    }
}