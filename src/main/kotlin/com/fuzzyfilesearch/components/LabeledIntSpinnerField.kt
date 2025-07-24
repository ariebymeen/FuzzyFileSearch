package com.fuzzyfilesearch.components

import com.intellij.ui.JBIntSpinner
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel

class LabeledIntSpinnerField(labelText: String, value: Int, minValue: Int, maxValue: Int, tooltipText: String = "") :
    JPanel(BorderLayout()) {
    val spinner = JBIntSpinner(value, minValue, maxValue)

    init {
        add(JLabel(labelText), BorderLayout.WEST)
        add(spinner, BorderLayout.CENTER)
        spinner.toolTipText = tooltipText
    }

    fun value(): Int {
        return spinner.value as Int
    }
}