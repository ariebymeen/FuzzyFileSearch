package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.renderers.VerticallyCenteredTextPane
import com.intellij.openapi.vfs.VirtualFile

class StringMatchInstanceItem(val vf: VirtualFile,
                              val start: Int,
                              val end: Int,
                              val text: String,
                              var panel: VerticallyCenteredTextPane? = null)

