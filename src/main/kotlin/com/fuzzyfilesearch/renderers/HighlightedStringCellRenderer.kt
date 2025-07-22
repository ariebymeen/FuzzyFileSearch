package com.fuzzyfilesearch.renderers

import com.fuzzyfilesearch.components.VerticallyCenteredTextPane
import com.fuzzyfilesearch.components.ShrunkVerticallyCenteredTextPane
import com.fuzzyfilesearch.searchbox.CustomRenderer
import com.fuzzyfilesearch.searchbox.getFont
import com.intellij.openapi.project.Project
import com.fuzzyfilesearch.settings.GlobalSettings
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.util.preferredWidth
import java.awt.*
import javax.swing.*
import javax.swing.text.StyleConstants
import cutoffStringToMaxWidth
import highlightText
import javax.swing.border.EmptyBorder
import javax.swing.text.Style

class HighlightedStringCellRenderer(val mProject: Project,
                                    val mSettings: GlobalSettings.SettingsState,
                                    val mShowFileName: Boolean) : CustomRenderer<StringMatchInstanceItem>() {
    val font = getFont(mSettings)
    lateinit var normalStyle: Style
    lateinit var tinyStyle: Style

    override fun getListCellRendererComponent(
        list: JList<out StringMatchInstanceItem>,
        value: StringMatchInstanceItem,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        if (value.mainPanel == null) {
            value.mainPanel = JPanel(BorderLayout())
            if (mSettings.string.showFileIcon) {
                value.iconLabel = JBLabel()
                value.iconLabel!!.border = EmptyBorder(0, 5, 0, 0)
                value.iconLabel!!.icon = value.vf.fileType.icon
                value.mainPanel?.add(value.iconLabel, BorderLayout.WEST)
            }
            value.textPane = VerticallyCenteredTextPane(mSettings.string.searchItemHeight)
            value.textPane!!.text = ""
            value.textPane!!.isOpaque = false
//            value.textPane?.preferredHeight = mSettings.string.searchItemHeight
//            value.textPane?.maximumHeight = mSettings.string.searchItemHeight
            value.textPane?.font = font
            normalStyle = value.textPane!!.addStyle("Normal", null).apply {}
            tinyStyle = value.textPane!!.addStyle("Tiny", null).apply {
                StyleConstants.setItalic(this, false)
                StyleConstants.setFontSize(this, StyleConstants.getFontSize(normalStyle) - 1)
            }

            val mShowFileName = true
            if (mShowFileName) {
                value.fileNameTextPane = ShrunkVerticallyCenteredTextPane(mSettings.string.searchItemHeight)
                value.fileNameTextPane!!.text = ""
                value.fileNameTextPane!!.isOpaque = false
                value.fileNameTextPane!!.styledDocument.insertString(0, value.vf.name, tinyStyle)
                if (mSettings.showLineNumberWithFileName) {
                    value.fileNameTextPane!!.styledDocument.insertString(value.fileNameTextPane!!.styledDocument.length,
                                                                                ":" + value.line_nr.toString(),
                                                                                tinyStyle)
                }
                value.mainPanel?.add(value.fileNameTextPane!!, BorderLayout.EAST)
                val fontMetrics: FontMetrics = value.fileNameTextPane!!.getFontMetrics(Font(font.family, Font.PLAIN, StyleConstants.getFontSize(tinyStyle)))
                value.fileNameWidth = fontMetrics.stringWidth(value.fileNameTextPane!!.text) + value.fileNameTextPane!!.margin.left + value.fileNameTextPane!!.margin.right
                value.fileNameTextPane!!.setWidth(value.fileNameWidth)
            }

            setText(value, index)
            value.mainPanel?.add(value.textPane, BorderLayout.CENTER)

        } else if (mSettings.string.showNumberInSearchView) {
            val formattedNumber = String.format(" %02d - ", index)
            value.textPane!!.styledDocument.remove(0, formattedNumber.length)
            value.textPane!!.styledDocument.insertString(0, formattedNumber, value.textPane!!.getStyle("Tiny"))
        }

        value.mainPanel!!.background = if (isSelected) list.selectionBackground else list.background
        value.mainPanel!!.foreground = if (isSelected) list.selectionForeground else list.foreground

        return value.mainPanel!!
    }

    fun setText(item: StringMatchInstanceItem, index: Int) {
        val doc = item.textPane!!.styledDocument
        if (mSettings.string.showNumberInSearchView) {
            val formattedNumber = String.format(" %02d - ", index)
            doc.insertString(doc.length, formattedNumber, tinyStyle)
        } else {
            doc.insertString(doc.length, " ", tinyStyle)
        }
        val highlightOffset = doc.length
        doc.insertString(doc.length, item.text, normalStyle)

        // If text is too wide for the view, remove and place ... at the end
        val iconW       = item.iconLabel?.preferredWidth ?: 0
        val fileNameW   = item.fileNameTextPane?.preferredWidth ?: 0
        cutoffStringToMaxWidth(item.textPane!!, doc, maxWidth - iconW - fileNameW)

        if (mSettings.applySyntaxHighlightingOnTextSearch) {
            highlightText(mProject, doc, highlightOffset , item.text, item.vf.extension)
        }
    }

}
