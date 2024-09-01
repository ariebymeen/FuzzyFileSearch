package com.quickfilesearch.searchbox

import com.intellij.openapi.vfs.VirtualFile
import com.quickfilesearch.settings.PathDisplayType
import java.awt.Component
import javax.swing.BorderFactory
import javax.swing.DefaultListCellRenderer
import javax.swing.JLabel
import javax.swing.JList

class SearchDialogCellRenderer(var pathRenderType: PathDisplayType,
                               var basePath: String) : DefaultListCellRenderer() {

    var emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);

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
        label.border = emptyBorder // Padding
        label.text = getLabelText(value, pathRenderType, index, basePath);

        return label
    }

    companion object {
        fun getLabelText(value: VirtualFile, pathRenderType: PathDisplayType, index: Int, basePath: String) : String {
            when (pathRenderType) {
                PathDisplayType.FILENAME_ONLY -> {
                    return "<html><b>$index: </b>   ${value.name}</html>"
                }
                PathDisplayType.FILENAME_RELATIVE_PATH -> {
                    val path = value.parent!!.path
                    return "<html><b>$index: </b>   <strong>${value.name}</strong>  <i>${path.subSequence(basePath.length, path.length)}</i></html>"
                }
                PathDisplayType.FILENAME_FULL_PATH -> {
                    return "<html><b>$index: </b>   <strong>${value.name}</strong>  <i>${value.parent!!.path}</i></html>"
                }
                PathDisplayType.FULL_PATH_WITH_FILENAME-> {
                    return "<html><b>$index: </b>   ${value.parent!!.path}/<strong>${value.name}</strong></html>"
                }
            }
        }
    }
}