package com.fuzzyfilesearch.renderers

import com.fuzzyfilesearch.components.VerticallyCenteredTextPane
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBPanel
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel

class FileInstanceItem(val vf: VirtualFile,
                       var mainPanel: JPanel? = null,
                       var icon: Icon? = null,
//                       var mainPanel: JBPanel<JBPanel<*>>? = null,
                       var iconLabel: JLabel? = null,
                       var textPane: VerticallyCenteredTextPane? = null)