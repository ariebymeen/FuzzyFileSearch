package com.fuzzyfilesearch.components

//import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
//import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JLabel
import javax.swing.JPanel
//import javax.swing.ScrollPaneConstants

class LabeledTextField(labelText: String, tooltipText: String = "", default: String = "") : JPanel(BorderLayout()) {
    val textField = JBTextField()
//    val scrollPane = JBScrollPane(textField).apply {
//        border = JBUI.Borders.empty()
//        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
//        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
//    }

    init {
        add(JLabel(labelText), BorderLayout.WEST)
//        add(scrollPane, BorderLayout.CENTER)
        add(textField, BorderLayout.CENTER)
        textField.toolTipText = tooltipText
        textField.text = default
    }

    fun text(): String {
        return textField.text.trim()
    }
}