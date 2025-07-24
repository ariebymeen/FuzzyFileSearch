package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.settings.GlobalSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


class ToggleDebugAction() : AnAction("FuzzyFileSearchToggleDebugOptions") {
    override fun actionPerformed(e: AnActionEvent) {
        val common = GlobalSettings().getInstance().state.common
        common.enableDebugOptions = !common.enableDebugOptions
        println("Toggled debug options. Debug options enabled: ${common.enableDebugOptions}")
    }
}