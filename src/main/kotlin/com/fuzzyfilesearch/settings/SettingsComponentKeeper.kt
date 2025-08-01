package com.fuzzyfilesearch.settings

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import javax.swing.JSpinner
import kotlin.reflect.KMutableProperty0

class SettingsComponentKeeper {
    val components = mutableListOf<SettingsComponent>()

    fun createCheckboxComponent(setting: KMutableProperty0<Boolean>, builder: FormBuilder, title: String, description: String) : JBCheckBox {
        val checkbox = JBCheckboxComponent(setting)
        builder.addLabeledComponent(createLabelWithDescription(title, description.trimIndent()), checkbox.checkbox)
        components.add(checkbox)
        return checkbox.checkbox
    }

    fun createJBIntSpinnerComponent(setting: KMutableProperty0<Int>, value: Int, minValue: Int, maxValue: Int, stepSize: Int,
                                    builder: FormBuilder, title: String, description: String) : JBIntSpinner {
        val spinner = JBIntSpinnerComponent(setting, value, minValue, maxValue, stepSize)
        builder.addLabeledComponent(createLabelWithDescription(title, description.trimIndent()), spinner.spinner)
        components.add(spinner)
        return spinner.spinner
    }

    fun createJSpinnerComponent(setting: KMutableProperty0<Double>, value: Double, minValue: Double, maxValue: Double, stepSize: Double,
                                builder: FormBuilder, title: String, description: String) : JSpinner {
        val spinner = JSpinnerComponent(setting, value, minValue, maxValue, stepSize)
        builder.addLabeledComponent(createLabelWithDescription(title, description.trimIndent()), spinner.spinner)
        components.add(spinner)
        return spinner.spinner
    }

    fun <E> createComboboxComponent(setting: KMutableProperty0<E>, values: Array<E>,
                                    builder: FormBuilder, title: String, description: String) : ComboBox<E> {
        val combobox = ComboBoxComponent<E>(setting, values)
        builder.addLabeledComponent(createLabelWithDescription(title, description.trimIndent()), combobox.combobox)
        components.add(combobox)
        return combobox.combobox
    }

    fun createTextFieldComponent(setting: KMutableProperty0<String>, builder: FormBuilder, title: String, description: String) {
        val textfield = JBTextFieldComponent(setting)
        builder.addLabeledComponent(createLabelWithDescription(title, description.trimIndent()), textfield.textfield)
        components.add(textfield)
    }

    fun createActionsTableComponent(setting: KMutableProperty0<Array<Array<String>>>, builder: FormBuilder, title: String, description: String,
                                    header: Array<String>, default: Array<String>, weights: Array<Int>, settings: GlobalSettings.SettingsState, nameIndex: Int, shortcutIndex: Int,
                                    createAction: (Array<Array<String>>, GlobalSettings.SettingsState) -> Unit) {
        val table = ActionsTableComponent(setting, header, default, weights, nameIndex, shortcutIndex, createAction, settings)
        builder.addComponent(createLabelWithDescription(title, description))
        builder.addComponent(table.table)
        components.add(table)
    }
}