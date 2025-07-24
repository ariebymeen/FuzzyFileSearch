package com.fuzzyfilesearch.components

import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class LabeledTextField(labelText: String, tooltipText: String = "", default: String = "") : JPanel(BorderLayout()) {
    val textField = JTextField()

    init {
        add(JLabel(labelText), BorderLayout.WEST)
        add(textField, BorderLayout.CENTER)
        textField.toolTipText = tooltipText
    }

    fun text(): String {
        return textField.text.trim()
    }
}