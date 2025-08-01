package com.fuzzyfilesearch.settings.actionView

import com.fuzzyfilesearch.actions.ActionType
import com.fuzzyfilesearch.actions.SearchRecentFilesAction
import com.fuzzyfilesearch.actions.utils
import com.fuzzyfilesearch.components.LabeledIntSpinnerField
import com.fuzzyfilesearch.components.LabeledTextField
import com.fuzzyfilesearch.components.WrappedCheckbox
import com.fuzzyfilesearch.settings.verifyActionName
import com.fuzzyfilesearch.settings.verifyShortcut
import javax.swing.JPanel

class SearchRecentFilesActionView() : ActionViewBase() {
    val historyLengthField =
            LabeledIntSpinnerField(
                "Number of files in history: ", 20, 1, 1000,
                "Enter the number of most recently opened files to remember")
    val extensionFilterField =
            LabeledTextField(
                "Extensions filter: ",
                "Enter a comma seperated list of extensions to filter (e.g. '.kt,.java')")
    val modifiedFilesOnlyCheckbox = WrappedCheckbox(
        "Only search files that have been modified",
        "If selected, only search through recent files that have been edited")
    val includeOpenFilesCheckbox = WrappedCheckbox(
        "Always include the open files in the search",
        "If selected, the recent files are first taken from the open files, then from the most recent (closed) files")

    override fun addToPanel(panel: JPanel) {
        panel.add(actionNameField)
        panel.add(historyLengthField)
        panel.add(extensionFilterField)
        panel.add(shortcutField)
        panel.add(modifiedFilesOnlyCheckbox)
        panel.add(includeOpenFilesCheckbox)
    }

    override fun initialize(settings: utils.ActionSettings) {
        actionNameField.textField.text = settings.name
        shortcutField.textField.text = settings.shortcut

        val custom = SearchRecentFilesAction.parseSettings(settings.generic)
        historyLengthField.spinner.value = custom.nofFilesHistory
        extensionFilterField.textField.text =
                if (custom.extensionList.isEmpty()) "" else custom.extensionList.joinToString(", ")
        modifiedFilesOnlyCheckbox.box.isSelected = custom.searchModifiedOnly
        includeOpenFilesCheckbox.box.isSelected = custom.alwaysIncludeOpenFiles

        initialSettings = this.getStored()
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
            ActionType.SEARCH_RECENT_FILES.toString(),
            actionNameField.text(),
            shortcutField.text(),
            historyLengthField.value().toString(),
            extensionFilterField.text(),
            modifiedFilesOnlyCheckbox.box.isSelected.toString(),
            includeOpenFilesCheckbox.box.isSelected.toString())
    }

    override fun help(): String {
        return """
            Open search popup to search through all most recently viewed files.
            Specify the number of files you want to remember and search over.
            Optionally filter to search only through files with specified extensions
        """.trimIndent()
    }
}
