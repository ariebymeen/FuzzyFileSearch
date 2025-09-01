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
            <b>Open a popup to quickly search for a file in the specified path.</b><br>
            Use this action if you want to:
            <ul>
                <li>Find files in a specific directory (or project root)</li>
                <li>Want to filter only on the file extension (optional)</li>
            </ul>
            <b>Name:</b> Enter a unique name, this is used to register the action in the intellij framework. The action will be registered as <i>com.fuzzyfilesearch.%NAME%</i><br>
            <b>Path:</b> Enter a path, where '/' is the project root. To search relative to the open file, start with './'<br>
            <b>Extensions filter (optional):</b> List the extensions to search over, seperated by ','. If empty, all file extensions are included in the search. <br>
            <b>Shortcut (optional):</b> Enter a shortcut to trigger the action <br>
        """.trimIndent()
    }
}
