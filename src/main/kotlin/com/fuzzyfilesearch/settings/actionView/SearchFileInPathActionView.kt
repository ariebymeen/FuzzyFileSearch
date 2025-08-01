package com.fuzzyfilesearch.settings.actionView

import com.fuzzyfilesearch.actions.ActionType
import com.fuzzyfilesearch.actions.SearchFileInPathAction
import com.fuzzyfilesearch.actions.utils
import com.fuzzyfilesearch.components.LabeledTextField
import com.fuzzyfilesearch.components.WrappedCheckbox
import com.fuzzyfilesearch.settings.verifyActionName
import com.fuzzyfilesearch.settings.verifyShortcut
import javax.swing.JPanel

class SearchFileInPathActionView() : ActionViewBase() {
    val pathField = LabeledTextField(
        "Path: ",
        "Enter a path. Starting with '/' searches from the project root, starting with '.' searches from the current file",
        "/")
    val extensionFilterField =
            LabeledTextField(
                "Extensions filter: ",
                "Enter a comma separated list of extensions to filter (e.g. '.kt,.java')")
    val vcsTrackedOnlyCheckbox = WrappedCheckbox(
        "Only search files tracked by vcs",
        "If selected, only search files that are tracked by vcs")
    val modifiedFilesOnlyCheckbox = WrappedCheckbox(
        "Only search files that have been modified",
        "If selected, only search through recent files that have been edited", false)

    override fun addToPanel(panel: JPanel) {
        panel.add(actionNameField)
        panel.add(pathField)
        panel.add(extensionFilterField)
        panel.add(shortcutField)
        panel.add(vcsTrackedOnlyCheckbox)
        panel.add(modifiedFilesOnlyCheckbox)
    }

    override fun initialize(settings: utils.ActionSettings) {
        actionNameField.textField.text = settings.name
        shortcutField.textField.text = settings.shortcut

        val custom = SearchFileInPathAction.parseSettings(settings.generic)
        pathField.textField.text = custom.path
        extensionFilterField.textField.text =
                if (custom.extensionList.isEmpty()) "" else custom.extensionList.joinToString(", ")
        vcsTrackedOnlyCheckbox.box.isSelected = custom.onlyVcsTracked
        modifiedFilesOnlyCheckbox.box.isSelected = custom.searchModifiedOnly

        initialSettings = this.getStored()
    }

    override fun modified(): Boolean {
        return !utils.isEqual(initialSettings, this.getStored())
    }

    override fun verify(): String {
        val res = verifyActionName(actionNameField.text())
        if (res.isNotEmpty()) return res

        if (pathField.text().isEmpty()) {
            return "Please provide a path"
        }

        val shortcut = shortcutField.text().trim()
        return verifyShortcut(shortcut)
    }

    override fun getStored(): Array<String> {
        return arrayOf(
            ActionType.SEARCH_FILE_IN_PATH.toString(),
            actionNameField.text(),
            shortcutField.text(),
            pathField.text(),
            extensionFilterField.text(),
            vcsTrackedOnlyCheckbox.box.isSelected.toString(),
            modifiedFilesOnlyCheckbox.box.isSelected.toString())
    }

    override fun help(): String {
        return """
            Open a popup to quickly search for a file in the specified path, with a certain extension (optional).
            The name is used to register the action (you can find and test this in Help->Search action)
            The path can be used to search in specific directories. To search from project root enter '/'.
            To search from the currently open file, start the path with '.' (for example '../' will search one directory higher)
        """.trimIndent()
    }
}
