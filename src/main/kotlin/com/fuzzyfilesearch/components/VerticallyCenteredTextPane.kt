package com.fuzzyfilesearch.components

import com.intellij.util.ui.JBUI
import java.awt.Graphics
import javax.swing.JTextPane

class VerticallyCenteredTextPane() : JTextPane() {
    var alignedText = false

    override fun paintComponent(g: Graphics) {
        if (!alignedText) {
            adjustVerticalCentering()
            alignedText = true
        }
        super.paintComponent(g)
    }

    fun adjustVerticalCentering() {
        val fm = this.getFontMetrics(font)
        val textHeight = fm.height * document.defaultRootElement.elementCount
        val yOffset = (height - textHeight) / 2
        margin = JBUI.insets(yOffset.coerceAtLeast(0), margin.left, margin.bottom, margin.right)
    }
}