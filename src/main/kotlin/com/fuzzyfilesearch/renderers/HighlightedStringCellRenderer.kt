package com.fuzzyfilesearch.renderers

import com.fuzzyfilesearch.actions.StringMatchInstanceItem
import com.fuzzyfilesearch.searchbox.CustomRenderer
import com.fuzzyfilesearch.searchbox.getFont
import com.intellij.openapi.project.Project
import com.intellij.ui.util.preferredHeight
import com.fuzzyfilesearch.settings.GlobalSettings
import com.intellij.ui.util.maximumHeight
import java.awt.*
import javax.swing.*
import javax.swing.text.StyleConstants
import cutoffStringToMaxWidth
import highlightText
import java.text.NumberFormat
import javax.swing.text.Style

class SimpleStringCellRenderer(val mProject: Project,
                               val mSettings: GlobalSettings.SettingsState) : CustomRenderer<StringMatchInstanceItem>() {
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
        if (value.panel == null) {
            value.panel = VerticallyCenteredTextPane()
            value.panel!!.text = ""
            value.panel!!.isOpaque = true
            value.panel?.preferredHeight = mSettings.string.searchItemHeight
            value.panel?.maximumHeight = mSettings.string.searchItemHeight
            value.panel?.font = font
            normalStyle = value.panel!!.addStyle("Normal", null).apply {}
            tinyStyle = value.panel!!.addStyle("Tiny", null).apply {
                StyleConstants.setItalic(this, false)
                StyleConstants.setFontSize(this, StyleConstants.getFontSize(normalStyle) - 1)
            }
            setText(value, index)
        } else if (mSettings.string.showNumberInSearchView) {
            val formattedNumber = String.format(" %02d - ", index)
            value.panel!!.styledDocument.remove(0, formattedNumber.length)
            value.panel!!.styledDocument.insertString(0, formattedNumber, value.panel!!.getStyle("Tiny"))
        }

        value.panel!!.background = if (isSelected) list.selectionBackground else list.background
        value.panel!!.foreground = if (isSelected) list.selectionForeground else list.foreground

        return value.panel!!
    }

    fun setText(item: StringMatchInstanceItem, index: Int) {

        val doc = item.panel!!.styledDocument

        if (mSettings.string.showNumberInSearchView) {
            val formattedNumber = String.format(" %02d - ", index)
            doc.insertString(doc.length, formattedNumber, tinyStyle)
        } else {
            doc.insertString(doc.length, " ", tinyStyle)
        }

        val offset = doc.length
        doc.insertString(doc.length, item.text, normalStyle)

        // If text is too wide for the view, remove and place ... at the end
        cutoffStringToMaxWidth(item.panel!!, doc, maxWidth)

        if (mSettings.applySyntaxHighlightingOnTextSearch) {
            highlightText(mProject, doc, offset, item.text, item.vf.extension)
        }
    }

}
