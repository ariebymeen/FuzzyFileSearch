package com.fuzzyfilesearch.components

import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel

class WrappedCheckbox(labelText: String, tooltipText: String = "", isSelected: Boolean = true) : JPanel(BorderLayout()) {
    val box = JBCheckBox(labelText)

    init {
        add(box, BorderLayout.WEST)
        box.isSelected = isSelected
        box.toolTipText = tooltipText
        box.border = JBUI.Borders.emptyLeft(0)
    }
}