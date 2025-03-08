package com.fuzzyfilesearch.searchbox

import com.fuzzyfilesearch.actions.GrepInstanceItem
import com.intellij.openapi.project.Project
import com.intellij.ui.util.preferredHeight
import com.intellij.util.ui.JBUI
import com.fuzzyfilesearch.settings.GlobalSettings
import com.fuzzyfilesearch.settings.PathDisplayType
import com.intellij.ui.util.maximumHeight
import java.awt.*
import javax.swing.*
import javax.swing.text.AttributeSet
import javax.swing.text.StyleConstants

class SimpleStringCellRenderer(val mProject: Project,
                               val mSettings: GlobalSettings.SettingsState) : CustomRenderer<GrepInstanceItem>() {
    val basePath = mProject.basePath!!
    val font = getFont(mSettings)

    override fun getListCellRendererComponent(
        list: JList<out GrepInstanceItem>,
        value: GrepInstanceItem,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        if (value.panel == null) {
            value.panel = VerticallyCenteredTextPane()
            value.panel!!.text = ""
            value.panel!!.isOpaque = true
            value.panel?.preferredHeight = mSettings.searchItemHeight
            value.panel?.maximumHeight = mSettings.searchItemHeight
            value.panel?.font = font
            setText(value, index)
        } else if (mSettings.showNumberInSearchView) {
            val formattedNumber = String.format(" %02d - ", index)
            value.panel!!.styledDocument.remove(0, formattedNumber.length)
            value.panel!!.styledDocument.insertString(0, formattedNumber, value.panel!!.getStyle("Tiny"))
        }

        value.panel!!.background = if (isSelected) list.selectionBackground else list.background
        value.panel!!.foreground = if (isSelected) list.selectionForeground else list.foreground

        return value.panel!!
    }

    fun setText(item: GrepInstanceItem, index: Int) {
        val normalStyle = item.panel!!.addStyle("Normal", null).apply {}
        val tinyStyle = item.panel!!.addStyle("Tiny", null).apply {
            StyleConstants.setItalic(this, false)
            StyleConstants.setFontSize(this, StyleConstants.getFontSize(normalStyle) - 1)
        }

        val doc = item.panel!!.styledDocument

        if (mSettings.showNumberInSearchView) {
            val formattedNumber = String.format(" %02d - ", index)
            doc.insertString(doc.length, formattedNumber, tinyStyle)
        } else {
            doc.insertString(doc.length, " ", tinyStyle)
        }
        doc.insertString(doc.length, "${item.match.result.value} ", normalStyle)

        // If text is too wide for the view, remove and place ... at the end
        val (nofCharsToRemove, nofDots) = computeNofCharsToRemove(item.panel!!, maxWidth)
        if (maxWidth > 0 && nofCharsToRemove > 0) {
            // Cutoff text. Compute the number of chars to remove + 3, which will be replaced by ...
            doc.remove(doc.length - nofCharsToRemove, nofCharsToRemove)
            val elem = doc.getCharacterElement(doc.length - 1)
            // insert ... with the same style as the last text
            doc.insertString(doc.length, ".".repeat(nofDots), elem.attributes)
        }
    }
}
