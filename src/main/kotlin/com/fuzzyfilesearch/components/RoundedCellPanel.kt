package com.fuzzyfilesearch.components

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JPanel

/**
 * Row panel used by the search-result renderers.
 *
 * A plain opaque JPanel gives each row a hard, full-width selection block.
 * Painting a rounded rectangle instead softens that block and leaves a little
 * breathing room around it, which matches the look of modern command palettes
 * while still using the IDE's own selection colors.
 *
 * The panel itself is non-opaque so the list background (and, for the preview
 * area, the surrounding panel) shows through between rows.
 */
class RoundedCellPanel : JPanel(BorderLayout()) {

    var selected: Boolean = false
    var hovered: Boolean = false

    var selectionColor: Color =
        JBColor.namedColor("List.selectionBackground", JBColor(0x3574F0, 0x4B6EAF))

    var hoverColor: Color =
        JBColor.namedColor("List.hoverBackground", JBColor(0xF2F3F5, 0x2E3032))

    init {
        isOpaque = false
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        if (!selected && !hovered) return
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON)
            val insetX = JBUI.scale(4)
            val insetY = JBUI.scale(1)
            val w = width - insetX * 2
            val h = height - insetY * 2
            if (w <= 0 || h <= 0) return
            g2.color = if (selected) selectionColor else hoverColor
            val arc = JBUI.scale(8)
            g2.fillRoundRect(insetX, insetY, w, h, arc, arc)
        } finally {
            g2.dispose()
        }
    }
}
