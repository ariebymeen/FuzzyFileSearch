package com.quickfilesearch.searchbox

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
    val cellBorder = BorderFactory.createEmtpyBorder(5,5,5,5) // Padding

    // Custom list cell renderer
    override fun getListCellRendererComponent(
        list: JList<*>,
        value: Any,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val value = value as VirtualFile;

        val label = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
        label.border = cellBorder 

        when (pathRenderType) {
            PathDisplayType.FILENAME_ONLY -> {
                label.text = "<html><b>$index: </b>   ${value.name}</html>"
            }
            PathDisplayType.FILENAME_RELATIVE_PATH -> {
                val path = value.parent!!.path
                if (isFileInProject(project, value)) {
                    label.text = "<html><b>$index: </b>   <strong>${value.name}</strong>  <i>${path.subSequence(basePath.length, path.length)}</i></html>"
                } else {
                    label.text = "<html><b>$index: </b>   <strong>${value.name}</strong>  <i>${path}</i></html>"
                }
            }
            PathDisplayType.FILENAME_FULL_PATH -> {
                label.text = "<html><b>$index: </b>   <strong>${value.name}</strong>  <i>${value.parent!!.path}</i></html>"
            }
            PathDisplayType.FULL_PATH_WITH_FILENAME-> {
                label.text = "<html><b>$index: </b>   ${value.parent!!.path}/<strong>${value.name}</strong></html>"
            }
        }

        return label
    }
}
