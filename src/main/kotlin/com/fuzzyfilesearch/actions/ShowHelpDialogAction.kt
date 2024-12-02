package com.fuzzyfilesearch.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.ProjectManager

class ShowHelpDialogAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project // Get the current project
        if (project != null) {
            TipsDialogView(project).show()
        }
        else {
            if (!ProjectManager.getInstance().openProjects.isEmpty()) {
                TipsDialogView(ProjectManager.getInstance().openProjects.first()).show()
            }
        }
    }
}
