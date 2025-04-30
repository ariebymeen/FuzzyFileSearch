package com.fuzzyfilesearch.searchbox

import com.intellij.ui.components.JBPanel
import java.awt.*
import javax.swing.JTextField
import javax.swing.border.EmptyBorder
import java.awt.Graphics2D

class TextFieldNoPadding(private val text: String) : JBPanel<TextFieldNoPadding>() {
    private val textField = JTextField(text).apply {
        horizontalAlignment = JTextField.CENTER
        border = EmptyBorder(0, 0, 0, 0)
        isEditable = false
        isOpaque = false
    }


    init {
        layout = BorderLayout()
        add(textField, BorderLayout.SOUTH)
    }


    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)


        val g2d = g.create() as Graphics2D
        g2d.color = Color.GRAY
        val lineHeight = height / 2


//        val fm = g2d.fontMetric
//        val textWidth = fm.stringWidth(text)
//        val textX = (width - textWidth) / 2

        val fm = g2d.fontMetrics
        val textWidth = fm.stringWidth(text)
        val textHeight = fm.height // Get the height of the text
        val textX = (width - textWidth) / 2


        g2d.drawLine(0, lineHeight, textX - 5, lineHeight)
        g2d.drawLine(textX + textWidth + 5, lineHeight, width, lineHeight)


        g2d.dispose()


        // Set preferred size to the height of the text
        preferredSize = Dimension(preferredSize.width, textHeight / 2)
//        preferredSize = Dimension(preferredSize.width, 0)
//        g2d.drawLine(0, lineHeight, textX - 5, lineHeight)
//        g2d.drawLine(textX + textWidth + 5, lineHeight, width, lineHeight)
//
//
//        g2d.dispose()
    }
}