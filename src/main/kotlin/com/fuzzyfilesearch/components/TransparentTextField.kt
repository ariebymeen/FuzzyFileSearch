package com.fuzzyfilesearch.components

import java.awt.AlphaComposite
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JTextField

class TransparentTextField(private val opacity: Float) : JTextField() {
    override fun paintComponent(g: Graphics) {
        val g2 = g as Graphics2D
        g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity)
        super.paintComponent(g2)
    }
}