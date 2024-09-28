package com.quickfilesearch.searchbox

import com.intellij.openapi.project.Project
import com.intellij.ui.util.preferredHeight
import com.intellij.util.ui.JBUI
import com.quickfilesearch.settings.GlobalSettings
import com.quickfilesearch.settings.PathDisplayType
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

class VerticallyCenteredTextPane : JTextPane() {
    var alignedText = false


    override fun paintComponent(g: Graphics) {
        // TODO: TEST
        //         val g2d = g as Graphics2D
        //
        //        // Enable anti-aliasing for smooth edges
        //        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        //
        //        // Draw the rounded background
        //        g2d.color = Color(200, 220, 240) // Light blue background color
        //        g2d.fillRoundRect(0, 0, width, height, 30, 30) // Adjust arc width and height for rounded corners
        //
        //        // Draw the border (optional)
        //        g2d.color = Color(100, 100, 150) // Border color
        //        g2d.stroke = BasicStroke(3f) // Border thickness
        //        g2d.drawRoundRect(0, 0, width - 1, height - 1, 30, 30) // Border with same rounded corner dimensions
        //
        //        // Call super to paint the text after the background
        //        super.paintComponent(g)
        //
        //        // Adjust vertical centering
        //        adjustVerticalCentering()
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

class SearchDialogCellRenderer(val mProject: Project,
                               val mSettings: GlobalSettings.SettingsState) : ListCellRenderer<PopupInstanceItem> {
    val basePath = mProject.basePath!!

    override fun getListCellRendererComponent(
        list: JList<out PopupInstanceItem>,
        value: PopupInstanceItem,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        if (value.panel == null) {
            value.panel = VerticallyCenteredTextPane()
            value.panel!!.text = ""
            value.panel!!.isOpaque = true
            value.panel?.preferredHeight = mSettings.searchItemHeight
            setText(value, index)
        } else {
            val formattedNumber = String.format("  %02d  -  ", index)
            value.panel!!.styledDocument.remove(0, formattedNumber.length)
            value.panel!!.styledDocument.insertString(0, formattedNumber, value.panel!!.getStyle("Tiny"))
        }

        value.panel!!.background = if (isSelected) list.selectionBackground else list.background
        value.panel!!.foreground = if (isSelected) list.selectionForeground else list.foreground

        return value.panel!!
    }

    fun setText(item: PopupInstanceItem, index: Int) {
        val italicStyle = item.panel!!.addStyle("Italic", null).apply {
            StyleConstants.setItalic(this, true)
        }
        val boldStyle = item.panel!!.addStyle("Bold", null).apply {
            StyleConstants.setBold(this, true)
        }
        val tinyStyle = item.panel!!.addStyle("Tiny", null).apply {
            StyleConstants.setItalic(this, false)
            StyleConstants.setFontSize(this, StyleConstants.getFontSize(boldStyle) - 1)
        }

        val doc = item.panel!!.styledDocument
        val formattedNumber = String.format("  %02d  -  ", index)
        doc.insertString(doc.length, formattedNumber, tinyStyle)
        when (mSettings.filePathDisplayType) {
            PathDisplayType.FILENAME_ONLY -> {
                doc.insertString(doc.length, "${item.vf.name} ", boldStyle)
            }

            PathDisplayType.FILENAME_RELATIVE_PATH -> {
                doc.insertString(doc.length, "${item.vf.name} ", boldStyle)
                val path = item.vf.parent!!.path
                if (isFileInProject(mProject, item.vf)) {
                    doc.insertString(doc.length, path.subSequence(basePath.length, path.length).toString(), italicStyle)
                } else {
                    doc.insertString(doc.length, path, italicStyle)
                }
            }

            PathDisplayType.FILENAME_FULL_PATH -> {
                doc.insertString(doc.length, "${item.vf.name} ", boldStyle)
                doc.insertString(doc.length, item.vf.parent!!.path, italicStyle)
            }

            PathDisplayType.FULL_PATH_WITH_FILENAME -> {
                doc.insertString(doc.length, "${item.vf.parent!!.path}/", italicStyle)
                doc.insertString(doc.length, "${item.vf.name} ", boldStyle)
            }

            PathDisplayType.RELATIVE_PATH_WITH_FILENAME -> {
                val path = item.vf.parent!!.path
                if (isFileInProject(mProject, item.vf)) {
                    doc.insertString(doc.length, path.subSequence(basePath.length, path.length).toString(), italicStyle)
                } else {
                    doc.insertString(doc.length, path, italicStyle)
                }
                doc.insertString(doc.length, "/", italicStyle)
                doc.insertString(doc.length, item.vf.name, boldStyle)
            }
        }
    }
}
