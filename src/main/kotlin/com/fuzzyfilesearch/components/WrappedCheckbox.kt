package com.fuzzyfilesearch.components

import com.intellij.ui.components.JBCheckBox
import java.awt.BorderLayout
import javax.swing.JPanel

class WrappedCheckbox(labelText: String, tooltipText: String = "") : JPanel(BorderLayout()) {
    val box = JBCheckBox(labelText)

    init {
        add(box, BorderLayout.WEST)
        box.isSelected = true
        box.toolTipText = tooltipText
    }
}