package com.fuzzyfilesearch.renderers

import com.fuzzyfilesearch.components.VerticallyCenteredTextPane
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.JLabel
import javax.swing.JPanel

class StringMatchInstanceItem(val vf: VirtualFile,
                              val start: Int,
                              val end: Int,
                              val text: String,
                              var mainPanel: JPanel? = null,
                              var iconLabel: JLabel? = null,
                              var textPane: VerticallyCenteredTextPane? = null,
                              var fileNameTextPane: VerticallyCenteredTextPane? = null,
                              var fileNameWidth: Int = 0)