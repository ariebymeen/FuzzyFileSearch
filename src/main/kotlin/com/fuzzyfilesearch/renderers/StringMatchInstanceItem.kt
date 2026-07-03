package com.fuzzyfilesearch.renderers

import com.fuzzyfilesearch.components.VerticallyCenteredTextPane
import com.fuzzyfilesearch.components.ShrunkVerticallyCenteredTextPane
import com.intellij.openapi.editor.Document
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextPane

class StringMatchInstanceItem(val vf: VirtualFile,
                              val start: Int,
                              val end: Int,
                              val line_nr: Int,
                              val text: String,
                              val document: Document? = null, // Document only used for full editor syntax highlighting
                              val precedingContent: String = "",
                              var icon: Icon? = null,
                              var mainPanel: JPanel? = null,
                              var iconLabel: JLabel? = null,
                              var textPane: VerticallyCenteredTextPane? = null,
                              var fileNameTextPane: ShrunkVerticallyCenteredTextPane? = null,
                              var fileNameWidth: Int = 0)