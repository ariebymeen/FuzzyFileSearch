import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import java.awt.Font
import java.awt.FontMetrics
import javax.swing.JTextPane
import javax.swing.text.*

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
//        println("0: Segment text: ${segmentText} has width $segmentWidth")
        if (totalWidth > maxWidth) {
            charsToRemove += segmentText.length
            break
        }
        if (totalWidth + segmentWidth > maxWidth) {
            // Compute the nof chars to remove. Start with initial guess from mean char width
            dotsTextWidth = fontMetrics.stringWidth(".")
            var nofRemovedChars: Int = Math.min(segmentText.length, (Math.max(segmentWidth + totalWidth - maxWidth, 1)) / segmentText.length)
            val segmentLen = segmentText.length
            try {
//                segmentText = doc.getText(elem.startOffset, elem.endOffset - elem.startOffset)
                segmentText = doc.getText(elem.startOffset, elem.endOffset - elem.startOffset - nofRemovedChars)
//                segmentText = doc.getText(elem.startOffset, Math.max(0, elem.endOffset - elem.startOffset - nofRemovedChars))
                segmentWidth = fontMetrics.stringWidth(segmentText)
//                println("1: Segment text: ${segmentText} has width $segmentWidth")
            } catch (e: BadLocationException) {
                println("Exception thrown: ${e.toString()}, segment text: ${segmentText}, elem start: ${elem.startOffset}, elem emd: ${elem.endOffset}, text: ${doc.getText(0, doc.length)}")
            }
            while (totalWidth + segmentWidth + dotsTextWidth * 2 > maxWidth && nofRemovedChars < segmentLen) {
                // Iteratively move to the chars until the text including '...' fits into the max width
                segmentWidth -= fontMetrics.stringWidth(segmentText.get(segmentLen - nofRemovedChars - 1).toString())
                ++nofRemovedChars
//                println("1: Segment text: ${segmentText} has width $segmentWidth. WL: ${totalWidth + segmentWidth + dotsTextWidth * 2}, nof rem cha: $nofRemovedChars, seg len: $segmentLen")
            }
            charsToRemove += nofRemovedChars
        }

        totalWidth += segmentWidth
        elementIndex = elem.endOffset + 1
    }

    // Now we can compute the number of dots that fit into the remaining space
    val spaceLeft = Math.max(0, maxWidth - totalWidth)
    val nofDots = Math.floor(spaceLeft / dotsTextWidth.toDouble()).toInt()

//    println("Text: ${doc.getText(0, doc.length)}, max width: ${maxWidth} space left: $spaceLeft, nof dots: $nofDots, dot w: $dotsTextWidth, nof chars to remove: $charsToRemove")
    return Pair(charsToRemove, nofDots)
}

fun cutoffStringToMaxWidth(item: JTextPane, doc: StyledDocument, maxWidth: Int) {
    val (nofCharsToRemove, nofDots) = computeNofCharsToRemove(item, maxWidth)
    if (maxWidth > 0 && nofCharsToRemove > 0) {
        // Cutoff text. Compute the number of chars to remove + 3, which will be replaced by ...
        doc.remove(doc.length - nofCharsToRemove, nofCharsToRemove)
        val elem = doc.getCharacterElement(doc.length - 1)
        // insert ... with the same style as the last text
        doc.insertString(doc.length, ".".repeat(nofDots), elem.attributes)
    }
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

fun getSyntaxHighlighterByExtension(project: Project, extension: String): SyntaxHighlighter? {
    val fileType = FileTypeManager.getInstance().getFileTypeByExtension(extension)
    return SyntaxHighlighterFactory.getSyntaxHighlighter(fileType, project, null)
}

fun getCurrentEditorColorScheme(): EditorColorsScheme {
    return EditorColorsManager.getInstance().globalScheme
}

fun highlightText(project: Project, doc: StyledDocument, startOffset: Int, text: String, extension: String?) {
    extension ?: return

    val syntaxHighlighter = getSyntaxHighlighterByExtension(project, extension)
    if (syntaxHighlighter == null) {
//        println("Error! Syntax highlighter not found for extension type: $text")
        return
    }

    val highlighter = LexerEditorHighlighter(syntaxHighlighter, getCurrentEditorColorScheme())
    highlighter.setText(text)

    val iterator = highlighter.createIterator(0)
    while (!iterator.atEnd()) {
        val textAttributesKey: TextAttributesKey? = syntaxHighlighter.getTokenHighlights(iterator.tokenType).firstOrNull()
        val textAttributes = textAttributesKey?.defaultAttributes
        if (textAttributes != null && textAttributes.foregroundColor != null) {
            val style = SimpleAttributeSet()
            StyleConstants.setForeground(style, textAttributes.foregroundColor)
            doc.setCharacterAttributes(iterator.start + startOffset, iterator.end - iterator.start, style, false)
        }

        iterator.advance()
    }
}
