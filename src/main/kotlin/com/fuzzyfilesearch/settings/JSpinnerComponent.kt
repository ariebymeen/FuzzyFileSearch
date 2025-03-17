package com.fuzzyfilesearch.settings

import com.intellij.ui.JBIntSpinner
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import kotlin.reflect.KMutableProperty0

class JSpinnerComponent(private var setting: KMutableProperty0<Double>, value: Double, minValue: Double, maxValue: Double, stepSize: Double): SettingsComponent {
    val spinner = JSpinner(SpinnerNumberModel(value, minValue, maxValue, stepSize))

    override fun initialize() {
        spinner.value = setting.get()
    }

    override fun modified(): Boolean {
        return spinner.value != setting.get()
    }

    override fun store() {
        setting.set(spinner.value as Double)
    }
}