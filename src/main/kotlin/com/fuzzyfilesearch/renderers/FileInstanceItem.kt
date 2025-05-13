package com.fuzzyfilesearch.renderers

import com.fuzzyfilesearch.components.VerticallyCenteredTextPane
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.JLabel
import javax.swing.JPanel

class FileInstanceItem(val vf: VirtualFile,
                           var mainPanel: JPanel? = null,
                           var iconLabel: JLabel? = null,
                           var textPane: VerticallyCenteredTextPane? = null)