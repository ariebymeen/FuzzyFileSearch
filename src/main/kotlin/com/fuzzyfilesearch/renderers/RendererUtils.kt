package com.fuzzyfilesearch.renderers

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import javax.swing.JTextPane
import javax.swing.text.*
import kotlin.math.max

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
            var nofRemovedChars: Int = Math.min(
                segmentText.length,
                (Math.max(segmentWidth + totalWidth - maxWidth, 1)) / segmentText.length)
            val segmentLen = segmentText.length
            try {
                segmentText = doc.getText(elem.startOffset, elem.endOffset - elem.startOffset - nofRemovedChars)
                segmentWidth = fontMetrics.stringWidth(segmentText)
            } catch (e: BadLocationException) {
                println("Exception thrown: ${e.toString()}, segment text: ${segmentText}, elem start: ${elem.startOffset}, elem emd: ${elem.endOffset}, text: ${doc.getText(0, doc.length)}")
            }
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
    val spaceLeft = Math.max(0, maxWidth - totalWidth)
    val nofDots = Math.floor(spaceLeft / dotsTextWidth.toDouble()).toInt()

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
        isBold             -> Font.BOLD
        isItalic           -> Font.ITALIC
        else               -> Font.PLAIN
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

fun applyTextAttributesToStyle(style: SimpleAttributeSet, attrs: TextAttributes) {
    if (attrs.foregroundColor != null) {
        StyleConstants.setForeground(style, attrs.foregroundColor)
    }
    if (attrs.backgroundColor != null) {
        StyleConstants.setBackground(style, attrs.backgroundColor)
    }
    val fontType = attrs.fontType
    StyleConstants.setBold(style, fontType == Font.BOLD || fontType == (Font.BOLD or Font.ITALIC))
    StyleConstants.setItalic(style, fontType == Font.ITALIC || fontType == (Font.BOLD or Font.ITALIC))
}


fun highlightText(
    project: Project,
    doc: StyledDocument,
    startOffset: Int,
    text: String,
    extension: String?,
    precedingContent: String = ""
) {
    extension ?: return
    if (text.isEmpty()) return

    val syntaxHighlighter = getSyntaxHighlighterByExtension(project, extension)
    if (syntaxHighlighter == null) {
        return
    }

    val scheme = getCurrentEditorColorScheme()
//    val contextLen = precedingContent.length
//    val fullText = precedingContent + text
    val fullText = text
    val textOffsetWithinFull = 0//contextLen

    val editorHighlighter = LexerEditorHighlighter(syntaxHighlighter, scheme)
    editorHighlighter.setText(fullText)

    val iterator = editorHighlighter.createIterator(0)
    while (!iterator.atEnd()) {
        val startInFull = iterator.start
        val endInFull = iterator.end
        val len = endInFull - startInFull
        if (len <= 0) {
            iterator.advance()
            continue
        }

        val visibleStart = startInFull - textOffsetWithinFull
        val visibleEnd = endInFull - textOffsetWithinFull
        val visibleStartClamped = max(0, visibleStart)
        val visibleEndClamped = minOf(text.length, visibleEnd)
        val visibleLen = visibleEndClamped - visibleStartClamped

        if (visibleLen > 0) {
            val attrs = iterator.textAttributes
            val style = SimpleAttributeSet()
            val keyArray: Array<TextAttributesKey> = iterator.textAttributesKeys
            var resolved: TextAttributes? = attrs
            if (resolved == null || resolved.isEmpty()) {
                for (k in keyArray) {
                    val v = scheme.getAttributes(k)
                    if (v != null && !v.isEmpty) {
                        resolved = v
                        break
                    }
                }
            }
            if (resolved != null && !resolved.isEmpty) {
                applyTextAttributesToStyle(style, resolved)
            } else {
                StyleConstants.setForeground(style, scheme.defaultForeground)
            }
            doc.setCharacterAttributes(startOffset + visibleStartClamped, visibleLen, style, true)
        }

        iterator.advance()
    }
}

private data class EditorHighlighterCacheValue(val range: List<CachedRange>, val docModStamp: Long)
private class EditorHighlighterCache {
    private val cachedHighlighters = mutableMapOf<VirtualFile, EditorHighlighterCacheValue>()

    fun getOrCreate(
        project: Project,
        file: VirtualFile,
        document: Document,
    ): List<CachedRange>? {
        val stamp = document.modificationStamp
        cachedHighlighters[file]?.let { if (it.docModStamp >= stamp) return it.range }

        val rangeList = ReadAction.compute<List<CachedRange>, Throwable> {
            val markup = DocumentMarkupModel.forDocument(document, project, true)

            val scheme = getCurrentEditorColorScheme()
            val result = mutableListOf<CachedRange>()
            for (h in markup.allHighlighters) {
                val hs = h.startOffset
                val he = h.endOffset
                val attrs: TextAttributes? = h.getTextAttributes(scheme)
                if (attrs == null || attrs.isEmpty) {
                    continue
                }
                val fg = attrs.foregroundColor
                val bg = attrs.backgroundColor
                val fontType = attrs.fontType
                val isBold = fontType == Font.BOLD || fontType == (Font.BOLD or Font.ITALIC)
                val isItalic = fontType == Font.ITALIC || fontType == (Font.BOLD or Font.ITALIC)
                result.add(
                    CachedRange(
                        start = hs,
                        end = he,
                        fg = fg,
                        bg = bg,
                        isBold = isBold,
                        isItalic = isItalic,
                    )
                )
            }
            result
        }

        println("Created cache for file ${file.path} with ${rangeList.size} range(s)")

        if (rangeList.isNotEmpty()) {
            cachedHighlighters[file] = EditorHighlighterCacheValue(rangeList, stamp)
        }
        return rangeList
    }

    fun dispose() {
        cachedHighlighters.clear()
    }
}

private data class CachedRange(
    val start: Int,
    val end: Int,
    val fg: Color?,
    val bg: Color?,
    val isBold: Boolean,
    val isItalic: Boolean,
)

private object EditorHighlighterCacheSingleton {
    val cache = EditorHighlighterCache()
}

fun highlightTextWithEditor(
    project: Project,
    doc: StyledDocument,
    startOffset: Int,
    text: String,
    file: VirtualFile,
    itemStart: Int,
    document: Document?
) {
    if (text.isEmpty()) return
    if (document == null) return

    val lineStart = itemStart
    val ranges = try {
        EditorHighlighterCacheSingleton.cache.getOrCreate(project, file, document)
    } catch (_: Throwable) { null } ?: return

    val scheme = getCurrentEditorColorScheme()
    for (r in ranges) {
        val visStart = max(0, r.start - lineStart)
        val visEnd = minOf(text.length, r.end - lineStart)
        if (visEnd <= visStart) continue
        val style = SimpleAttributeSet()
        if (r.fg != null) StyleConstants.setForeground(style, r.fg)
        else StyleConstants.setForeground(style, scheme.defaultForeground)
        if (r.bg != null) StyleConstants.setBackground(style, r.bg)
        StyleConstants.setBold(style, r.isBold)
        StyleConstants.setItalic(style, r.isItalic)
        doc.setCharacterAttributes(startOffset + visStart, visEnd - visStart, style, true)
    }
}
