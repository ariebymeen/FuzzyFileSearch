package com.quickfilesearch.searchbox

import com.intellij.openapi.project.Project
import com.quickfilesearch.settings.PathDisplayType
import java.awt.Component
import javax.swing.*
import javax.swing.text.StyleConstants

class SearchDialogCellRenderer(val pathRenderType: PathDisplayType,
                               val basePath: String,
                               val project: Project) : ListCellRenderer<PopupInstanceItem> {

    override fun getListCellRendererComponent(
        list: JList<out PopupInstanceItem>,
        value: PopupInstanceItem,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        if (value.panel == null) {
            value.panel = JTextPane()

            value.panel!!.text = "" // Clear previous content
            setText(value, index)
            value.panel!!.isOpaque = true // Ensure the background color is painted
        } else {
            val formattedNumber = String.format("%02d  -  ", index)
            value.panel!!.styledDocument.remove(0, formattedNumber.length)
            value.panel!!.styledDocument.insertString(0, formattedNumber, value.panel!!.getStyle("Bold"))
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

        val formattedNumber = String.format("%02d  -  ", index)
        val doc = item.panel!!.styledDocument
        when (pathRenderType) {
            PathDisplayType.FILENAME_ONLY -> {
                doc.insertString(doc.length, "$formattedNumber${item.vf.name} ", boldStyle)
            }

            PathDisplayType.FILENAME_RELATIVE_PATH -> {
                doc.insertString(doc.length, "$formattedNumber${item.vf.name} ", boldStyle)
                val path = item.vf.parent!!.path
                if (isFileInProject(project, item.vf)) {
                    doc.insertString(doc.length, path.subSequence(basePath.length, path.length).toString(), italicStyle)
                } else {
                    doc.insertString(doc.length, path, italicStyle)
                }
            }

            PathDisplayType.FILENAME_FULL_PATH -> {
                doc.insertString(doc.length, "$formattedNumber${item.vf.name} ", boldStyle)
                doc.insertString(doc.length, item.vf.parent!!.path, italicStyle)
            }

            PathDisplayType.FULL_PATH_WITH_FILENAME -> {
                doc.insertString(doc.length, item.vf.parent!!.path, italicStyle)
                doc.insertString(doc.length, "$formattedNumber${item.vf.name} ", boldStyle)
            }

            PathDisplayType.RELATIVE_PATH_WITH_FILENAME -> {
                doc.insertString(doc.length, formattedNumber, boldStyle)
                val path = item.vf.parent!!.path
                if (isFileInProject(project, item.vf)) {
                    doc.insertString(doc.length, path.subSequence(basePath.length, path.length).toString(), italicStyle)
                } else {
                    doc.insertString(doc.length, path, italicStyle)
                }
                doc.insertString(doc.length, "/", italicStyle)
                doc.insertString(doc.length, item.vf.name, boldStyle)
            }
        }
    }

//    companion object {
//
//        fun getLabelText(value: PopupInstanceItem, index: Int) : String {
//            return "<html><b>$index:   </b> ${value.html}</html>"
//        }
//
//        fun getLabelHtml(file: VirtualFile, pathRenderType: PathDisplayType, project: Project) : String {
//            val basePath = project.basePath ?: "";
//            when (pathRenderType) {
//                PathDisplayType.FILENAME_ONLY -> {
//                    return file.name
//                }
//                PathDisplayType.FILENAME_RELATIVE_PATH -> {
//                    val path = file.parent!!.path
//                    return if (isFileInProject(project, file)) {
//                        "<strong>${file.name}</strong>  <i>${path.subSequence(basePath.length, path.length)}</i>"
//                    } else {
//                        "<strong>${file.name}</strong>  <i>${path}</i>"
//                    }
//                }
//                PathDisplayType.FILENAME_FULL_PATH -> {
//                    return "<strong>${file.name}</strong>  <i>${file.parent!!.path}</i>"
//                }
//                PathDisplayType.FULL_PATH_WITH_FILENAME -> {
//                    return "${file.parent!!.path}/<strong>${file.name}</strong>"
//                }
//                PathDisplayType.RELATIVE_PATH_WITH_FILENAME -> {
//                    val path = file.parent!!.path
//                    return if (isFileInProject(project, file)) {
//                        "<i>${path.subSequence(basePath.length, path.length)}/</i><strong>${file.name}</strong>"
//                    } else {
//                        "<i>${path}/</i><strong>${file.name}</strong></html>"
//                    }
//                }
//            }
//
//        }
//    }

}
