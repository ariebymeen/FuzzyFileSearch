package com.fuzzyfilesearch.renderers

import com.fuzzyfilesearch.actions.GrepInstanceItem
import com.fuzzyfilesearch.searchbox.CustomRenderer
import com.fuzzyfilesearch.searchbox.PopupInstanceItem
import com.fuzzyfilesearch.searchbox.getFont
import com.fuzzyfilesearch.searchbox.isFileInProject
import com.fuzzyfilesearch.settings.GlobalSettings
import com.fuzzyfilesearch.settings.PathDisplayType
import com.intellij.openapi.project.Project
import com.intellij.ui.util.maximumHeight
import com.intellij.ui.util.preferredHeight
import cutoffStringToMaxWidth
import java.awt.*
import javax.swing.JList
import javax.swing.text.StyleConstants

class SearchDialogCellRenderer(val mProject: Project,
                               val mSettings: GlobalSettings.SettingsState) : CustomRenderer<PopupInstanceItem>() {
    val basePath = mProject.basePath!!
    val font = getFont(mSettings)

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
        if (mSettings.showNumberInSearchView) {
            val formattedNumber = String.format(" %02d - ", index)
            doc.insertString(doc.length, formattedNumber, tinyStyle)
        } else {
            doc.insertString(doc.length, " ", tinyStyle)
        }
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

        cutoffStringToMaxWidth(item.panel!!, doc, maxWidth)
    }
}
