package com.fuzzyfilesearch.renderers

import com.fuzzyfilesearch.components.VerticallyCenteredTextPane
import com.fuzzyfilesearch.searchbox.CustomRenderer
import com.fuzzyfilesearch.searchbox.getFont
import com.fuzzyfilesearch.settings.GlobalSettings
import com.fuzzyfilesearch.settings.PathDisplayType
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.util.preferredWidth
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.text.Style
import javax.swing.text.StyleConstants

class FilePathCellRenderer(
    val mProject: Project,
    val mSettings: GlobalSettings.SettingsState,
    var searchPath: String) : CustomRenderer<FileInstanceItem>() {
    val font = getFont(mSettings)
    lateinit var tinyStyle: Style
    lateinit var boldStyle: Style
    lateinit var italicStyle: Style

    init {
        if (searchPath.takeLast(1) == "/") {
            searchPath = searchPath.subSequence(0, searchPath.length - 1).toString()
        }
    }

    override fun getListCellRendererComponent(
        list: JList<out FileInstanceItem>,
        value: FileInstanceItem,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean): Component {

        if (value.textPane == null) {
            value.mainPanel = JPanel(BorderLayout())
            if (mSettings.file.showFileIcon) {
                value.iconLabel = JBLabel()
                value.iconLabel!!.border = JBUI.Borders.emptyLeft(5)
                value.iconLabel!!.icon = value.icon
                value.mainPanel?.add(value.iconLabel!!, BorderLayout.WEST)
            }
            value.textPane = VerticallyCenteredTextPane(mSettings.file.searchItemHeight)
            value.textPane!!.text = ""
            value.textPane!!.isOpaque = false
            value.textPane?.font = font
            italicStyle = value.textPane!!.addStyle("Italic", null).apply {
                StyleConstants.setItalic(this, true)
            }
            boldStyle = value.textPane!!.addStyle("Bold", null).apply {
                StyleConstants.setBold(this, true)
            }
            tinyStyle = value.textPane!!.addStyle("Tiny", null).apply {
                StyleConstants.setItalic(this, false)
                StyleConstants.setFontSize(this, StyleConstants.getFontSize(boldStyle) - 1)
            }
            setText(value, index)
            value.mainPanel?.add(value.textPane) // TODO: REMOVE
        } else if (mSettings.file.showNumberInSearchView) {
            val formattedNumber = String.format(" %02d - ", index)
            value.textPane!!.styledDocument.remove(0, formattedNumber.length)
            value.textPane!!.styledDocument.insertString(0, formattedNumber, value.textPane!!.getStyle("Tiny"))
        }

        value.mainPanel!!.background = if (isSelected) list.selectionBackground else list.background
        value.mainPanel!!.foreground = if (isSelected) list.selectionForeground else list.foreground

        return value.mainPanel!!
    }

    fun setText(item: FileInstanceItem, index: Int) {

        val doc = item.textPane!!.styledDocument
        if (mSettings.file.showNumberInSearchView) {
            val formattedNumber = String.format(" %02d - ", index)
            doc.insertString(doc.length, formattedNumber, tinyStyle)
        } else {
            doc.insertString(doc.length, " ", tinyStyle)
        }
        when (mSettings.filePathDisplayType) {
            PathDisplayType.FILENAME_ONLY               -> {
                doc.insertString(doc.length, "${item.vf.name} ", boldStyle)
            }

            PathDisplayType.FILENAME_RELATIVE_PATH      -> {
                doc.insertString(doc.length, "${item.vf.name} ", boldStyle)
                val path = item.vf.parent!!.path
                if (path.length >= searchPath.length) {
                    doc.insertString(doc.length, path.subSequence(searchPath.length, path.length).toString(), italicStyle)
                } else {
                    doc.insertString(doc.length, path, italicStyle)
                }
                doc.insertString(doc.length, "/", italicStyle)
            }

            PathDisplayType.FILENAME_FULL_PATH          -> {
                doc.insertString(doc.length, "${item.vf.name} ", boldStyle)
                doc.insertString(doc.length, item.vf.parent!!.path, italicStyle)
            }

            PathDisplayType.FULL_PATH_WITH_FILENAME     -> {
                doc.insertString(doc.length, "${item.vf.parent!!.path}/", italicStyle)
                doc.insertString(doc.length, "${item.vf.name} ", boldStyle)
            }

            PathDisplayType.RELATIVE_PATH_WITH_FILENAME -> {
                val path = item.vf.parent!!.path
                if (path.length >= searchPath.length) {
                    doc.insertString(doc.length, path.subSequence(searchPath.length, path.length).toString(), italicStyle)
                } else {
                    doc.insertString(doc.length, path, italicStyle)
                }
                doc.insertString(doc.length, "/", italicStyle)
                doc.insertString(doc.length, item.vf.name, boldStyle)
            }
        }

        val iconW = item.iconLabel?.preferredWidth ?: 0
        cutoffStringToMaxWidth(item.textPane!!, doc, maxWidth - iconW)
    }
}
