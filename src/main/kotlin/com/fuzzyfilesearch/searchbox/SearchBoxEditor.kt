package com.fuzzyfilesearch.searchbox

/*
MIT License

Copyright (c) 2024 Mitja Leino

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorTextField
import javax.swing.BorderFactory

class SearchBoxEditor(project: Project?) : EditorTextField(project, PlainTextFileType.INSTANCE) {
    private var currentFile: VirtualFile? = null

    public override fun createEditor(): EditorEx {
        val editor = super.createEditor()
        editor.setVerticalScrollbarVisible(true)
        editor.setHorizontalScrollbarVisible(true)
        editor.isOneLineMode = false
        editor.settings.isLineNumbersShown = true
        editor.isViewer = false
        // TODO: Make this editable
        editor.foldingModel.isFoldingEnabled = false
        editor.setBorder(BorderFactory.createEmptyBorder())
        editor.scrollPane.border = BorderFactory.createEmptyBorder(2, 4, 2, 4)

        val globalScheme = EditorColorsManager.getInstance().globalScheme
        font = globalScheme.getFont(null)
        editor.colorsScheme = globalScheme // Apply the current global editor color scheme

        // Set the background color explicitly to match the main editor
        val backgroundColor = globalScheme.getColor(EditorColors.CARET_ROW_COLOR) ?: globalScheme.defaultBackground
        editor.backgroundColor =  backgroundColor

//        val previewFontSize = settingsState.previewFontSize
//        if (previewFontSize != 0) {
//            this.font = this.font.deriveFont(previewFontSize.toFloat())
//        }

        return editor
    }

    fun updateFile(virtualFile: VirtualFile?, caretOffset: Int = 0) {
        if (currentFile == virtualFile) {
            ApplicationManager.getApplication().invokeLater { moveToOffset(caretOffset) }
            return
        }
        currentFile = virtualFile

        ApplicationManager.getApplication().executeOnPooledThread {
            val sourceDocument = ApplicationManager.getApplication().runReadAction<Document?> {
                virtualFile?.let { FileDocumentManager.getInstance().getDocument(virtualFile) }
            }
            val fileType = virtualFile?.let { FileTypeManager.getInstance().getFileTypeByFile(virtualFile) }

            ApplicationManager.getApplication().invokeLater {
                if (sourceDocument != null) {
                    WriteCommandAction.runWriteCommandAction(project) {
                        // Use the actual document to enable full highlighting in the preview
                        document = sourceDocument
                        ApplicationManager.getApplication().invokeLater { moveToOffset(caretOffset) }
                    }
                } else {
                    document = EditorFactory.getInstance().createDocument("Cannot preview file")
                }
                if (fileType != null) this.fileType = fileType
            }
        }
    }

    fun moveToOffset(offset: Int) {
        val editor = this.editor as? EditorEx ?: return
        editor.caretModel.moveToOffset(offset)
        editor.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.CENTER_UP)
    }

//    fun moveToLine(lineNumber: Int) {
//        val editor = this.editor as? EditorEx ?: return
//        val document = editor.document
//        if (lineNumber in 1..document.lineCount) {
//            val offset = document.getLineStartOffset(lineNumber - 1)
//            editor.caretModel.moveToOffset(offset)
//            editor.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.CENTER)
//        }
//    }
}