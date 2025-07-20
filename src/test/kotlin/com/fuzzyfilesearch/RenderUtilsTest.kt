package com.fuzzyfilesearch
import computeNofCharsToRemove
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import javax.swing.JTextPane
import javax.swing.text.StyleConstants

class RenderUtilsTest {
    @Test
    @DisplayName("Test cutoff point of string")
    fun test_string_cutoff() {
        val text = "return extension.split('|', ',', ';', ':').map { ext -> ext.replace('.', ' ').trim().lowercase() }"
//        val text = "import com.intellij.openapi.actionSystem.ActionManager"
        val maxW = 770

        val panel = JTextPane()
        val normalStyle = panel.addStyle("Normal", null).apply {
            StyleConstants.setFontSize(this, 12)
        }
        val tinyStyle = panel.addStyle("Tiny", null).apply {
            StyleConstants.setItalic(this, false)
            StyleConstants.setFontSize(this, 11)
        }

        val doc = panel.styledDocument
        val formattedNumber = String.format(" %02d - ", 1)
        doc.insertString(0, formattedNumber, tinyStyle)
        doc.insertString(doc.length, text, normalStyle)
//        fun computeNofCharsToRemove(textPane: JTextPane, maxWidth: Int): Pair<Int, Int> {
        val (nofCharsToRemove, nofDots) = computeNofCharsToRemove(panel, maxW)
        println("Nof chars: $nofCharsToRemove, nof dots: $nofDots")

    }
}