package com.fuzzyfilesearch.components

import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JTextPane

open class VerticallyCenteredTextPane(val heigth: Int) : JTextPane() {
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

    override fun getMaximumSize(): Dimension? {
        return Dimension(Int.MAX_VALUE, heigth)
    }

    override fun getPreferredSize(): Dimension? {
        return Dimension(Int.MAX_VALUE, heigth)
    }
}

class ShrunkVerticallyCenteredTextPane(height: Int) : VerticallyCenteredTextPane(height) {
    var maxWidth: Int = Int.MAX_VALUE

    fun setWidth(width: Int) {
        maxWidth = width
    }

    override fun getMaximumSize(): Dimension? {
        return Dimension(maxWidth, heigth)
    }

    override fun getPreferredSize(): Dimension? {
        return Dimension(maxWidth, heigth)
    }
}
