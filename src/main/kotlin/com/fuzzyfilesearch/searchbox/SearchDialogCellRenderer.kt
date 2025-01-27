package com.fuzzyfilesearch.searchbox

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

fun computeNofCharsToRemove(textPane: JTextPane, maxWidth: Int): Pair<Int, Int> {
    val doc = textPane.styledDocument

    var totalWidth = 0
    var charsToRemove = 0
    var dotsTextWidth = 0

    var elementIndex = 0
    val nofChars = doc.length
    while (elementIndex < nofChars) {
        val elem = doc.getCharacterElement(elementIndex)
        val fontStyle = getFontFromAttributes(elem.attributes, textPane.font)
        val fontMetrics: FontMetrics = textPane.getFontMetrics(fontStyle)
        var segmentText = doc.getText(elem.startOffset, elem.endOffset - elem.startOffset)
        var segmentWidth = fontMetrics.stringWidth(segmentText)
        if (totalWidth > maxWidth) {
            charsToRemove += segmentText.length
            break
        }
        if (totalWidth + segmentWidth > maxWidth) {
            // Compute the nof chars to remove. Start with initial guess from mean char width
            dotsTextWidth = fontMetrics.stringWidth(".")
            var nofRemovedChars: Int = (Math.max(segmentWidth + totalWidth - maxWidth, 1)) / segmentText.length
            val segmentLen = segmentText.length
            segmentText = doc.getText(elem.startOffset, elem.endOffset - elem.startOffset - nofRemovedChars)
            segmentWidth = fontMetrics.stringWidth(segmentText)
            while (totalWidth + segmentWidth + dotsTextWidth * 2 > maxWidth && nofRemovedChars < segmentLen) {
                // Iteratively move to the chars until the text including '...' fits into the max width
                segmentWidth -= fontMetrics.stringWidth(segmentText.get(segmentLen - nofRemovedChars - 1).toString())
                ++nofRemovedChars
            }
            charsToRemove += nofRemovedChars
        }

        totalWidth += segmentWidth
        elementIndex = elem.endOffset + 1
    }

    // Now we can compute the number of dots that fit into the remaining space
    val spaceLeft = maxWidth - totalWidth
    val nofDots = Math.floor(spaceLeft / dotsTextWidth.toDouble()).toInt()

    return Pair(charsToRemove, nofDots)
}

// Helper function to get a Font object based on style attributes
fun getFontFromAttributes(attrs: AttributeSet, defaultFont: Font): Font {
    val isBold = StyleConstants.isBold(attrs)
    val isItalic = StyleConstants.isItalic(attrs)
    val fontSize = StyleConstants.getFontSize(attrs)
    val fontFamily = StyleConstants.getFontFamily(attrs) ?: defaultFont.family

    // Create a new font with the specified family, style, and size
    val style = when {
        isBold && isItalic -> Font.BOLD or Font.ITALIC
        isBold -> Font.BOLD
        isItalic -> Font.ITALIC
        else -> Font.PLAIN
    }

    return Font(fontFamily, style, fontSize)
}

class VerticallyCenteredTextPane() : JTextPane() {
    var alignedText = false

    override fun paintComponent(g: Graphics) {
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
    var maxWidth = 0

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
            value.panel?.baselineResizeBehavior
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

        // If text is too wide for the view, remove and place ... at the end
        val (nofCharsToRemove, nofDots) = computeNofCharsToRemove(item.panel!!, maxWidth)
        if (maxWidth > 0 && nofCharsToRemove > 0) {
            // Cutoff text. Compute the number of chars to remove + 3, which will be replaced by ...
            doc.remove(doc.length - nofCharsToRemove, nofCharsToRemove)
            val elem = doc.getCharacterElement(doc.length - 1)
            // insert ... with the same style as the last text
            doc.insertString(doc.length, ".".repeat(nofDots), elem.attributes)
        }
        if (maxWidth == 0) {
            println("Max width zero!")
        }
    }
}
