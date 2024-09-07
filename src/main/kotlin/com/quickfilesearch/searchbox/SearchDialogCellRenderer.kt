package com.quickfilesearch.searchbox

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.quickfilesearch.settings.PathDisplayType
import java.awt.Component
import javax.swing.BorderFactory
import javax.swing.DefaultListCellRenderer
import javax.swing.JLabel
import javax.swing.JList

class SearchDialogCellRenderer(var pathRenderType: PathDisplayType,
                               var basePath: String,
                               var project: Project) : DefaultListCellRenderer() {
    var emptyBorder = BorderFactory.createEmptyBorder(3, 3, 3, 3)

    override fun getListCellRendererComponent(
        list: JList<*>,
        value: Any,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val vf = value as VirtualFile

        val label = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
        label.border = emptyBorder // Padding
        label.text = getLabelText(vf, pathRenderType, index, basePath, project)

        return label
    }

    companion object {
        fun isFileModified(virtualFile: VirtualFile): Boolean {
            val document = FileDocumentManager.getInstance().getDocument(virtualFile)
            return document != null && FileDocumentManager.getInstance().isDocumentUnsaved(document)
        }

        fun getLabelText(value: VirtualFile, pathRenderType: PathDisplayType, index: Int, basePath: String, project: Project) : String {
            val indicator = if (isFileModified(value)) "*" else ""
            when (pathRenderType) {
                PathDisplayType.FILENAME_ONLY -> {
                    return "<html><b>$index: </b>   $indicator${value.name}</html>"
                }
                PathDisplayType.FILENAME_RELATIVE_PATH -> {
                    val path = value.parent!!.path
                    return if (isFileInProject(project, value)) {
                        "<html><b>$index: </b>   <strong>$indicator${value.name}</strong>  <i>${path.subSequence(basePath.length, path.length)}</i></html>"
                    } else {
                        "<html><b>$index: </b>   <strong>$indicator${value.name}</strong>  <i>${path}</i></html>"
                    }
                }
                PathDisplayType.FILENAME_FULL_PATH -> {
                    return "<html><b>$index: </b>   <strong>$indicator${value.name}</strong>  <i>${value.parent!!.path}</i></html>"
                }
                PathDisplayType.FULL_PATH_WITH_FILENAME-> {
                    return "<html><b>$index: </b>   $indicator${value.parent!!.path}/<strong>${value.name}</strong></html>"
                }
            }
        }
    }
}
