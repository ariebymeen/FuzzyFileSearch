package com.fuzzyfilesearch.settings.actionView

import com.fuzzyfilesearch.actions.ActionType
import com.fuzzyfilesearch.actions.SearchOpenFilesAction
import com.fuzzyfilesearch.actions.utils
import com.fuzzyfilesearch.components.LabeledTextField
import com.fuzzyfilesearch.settings.verifyActionName
import com.fuzzyfilesearch.settings.verifyShortcut
import javax.swing.JPanel

class SearchOpenFilesActionView() : ActionViewBase() {
    val extensionFilterField =
            LabeledTextField(
                "Extensions filter: ",
                "Enter a comma seperated list of extensions to filter (e.g. '.kt,.java')")

    override fun addToPanel(panel: JPanel) {
        panel.add(actionNameField)
        panel.add(extensionFilterField)
        panel.add(shortcutField)
    }

    override fun initialize(panel: JPanel, settings: utils.ActionSettings) {
        actionNameField.textField.text = settings.name
        shortcutField.textField.text = settings.shortcut

        val custom = SearchOpenFilesAction.parseSettings(settings.generic)
        extensionFilterField.textField.text =
                if (custom.extensionList.isEmpty()) "" else custom.extensionList.joinToString(", ")

        initialSettings = this.getStored()
        this.addToPanel(panel)
    }

    override fun modified(): Boolean {
        return !utils.isEqual(initialSettings, this.getStored())
    }

    override fun verify(): String {
        val res = verifyActionName(actionNameField.text())
        if (res.isNotEmpty()) return res

        val shortcut = shortcutField.text().trim()
        return verifyShortcut(shortcut)
    }

    override fun getStored(): Array<String> {
        return arrayOf(
            ActionType.SEARCH_OPEN_FILES.toString(),
            actionNameField.text(),
            shortcutField.text(),
            extensionFilterField.text())
    }

    override fun help(): String {
        return """
            Open search popup view to search through all files currently opened
        """.trimIndent()
    }
}
