package com.fuzzyfilesearch.settings

import com.intellij.ui.JBIntSpinner
import kotlin.reflect.KMutableProperty0

class JBIntSpinnerComponent(private var setting: KMutableProperty0<Int>, value: Int, minValue: Int, maxValue: Int, stepSize: Int): SettingsComponent {
    val spinner = JBIntSpinner(value, minValue, maxValue, stepSize)

    override fun initialize() {
        spinner.value = setting.get()
    }

    override fun modified(): Boolean {
        return spinner.value != setting.get()
    }

    override fun store() {
        setting.set(spinner.value as Int)
    }
}