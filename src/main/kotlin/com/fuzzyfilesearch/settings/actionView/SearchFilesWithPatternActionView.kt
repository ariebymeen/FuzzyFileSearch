package com.fuzzyfilesearch.settings.actionView

import com.fuzzyfilesearch.actions.ActionType
import com.fuzzyfilesearch.actions.SearchFilesWithPatternAction
import com.fuzzyfilesearch.actions.utils
import com.fuzzyfilesearch.components.LabeledTextField
import com.fuzzyfilesearch.components.WrappedCheckbox
import com.fuzzyfilesearch.settings.verifyActionName
import com.fuzzyfilesearch.settings.verifyRegex
import com.fuzzyfilesearch.settings.verifyShortcut
import javax.swing.JPanel

class SearchFilesWithPatternActionView() : ActionViewBase() {
    val pathField = LabeledTextField(
        "Path: ",
        "Enter a path. Starting with '/' searches from the project root, starting with '.' searches from the current file",
        "/")
    val regexPatternField =
            LabeledTextField(
                "Regex: ",
                "Enter a regex to find files with a matching name. (e.g. '[A-Za-z]+Test.cc' returns all files ending with Test.cc)")
    val vcsTrackedOnlyCheckbox = WrappedCheckbox(
        "Only search files tracked by vcs",
        "If selected, only search files that are tracked by vcs")

    override fun addToPanel(panel: JPanel) {
        panel.add(actionNameField)
        panel.add(pathField)
        panel.add(regexPatternField)
        panel.add(shortcutField)
        panel.add(vcsTrackedOnlyCheckbox)
    }

    override fun initialize(settings: utils.ActionSettings) {
        actionNameField.textField.text = settings.name
        shortcutField.textField.text = settings.shortcut

        val custom = SearchFilesWithPatternAction.parseSettings(settings.generic)
        pathField.textField.text = custom.path
        regexPatternField.textField.text = custom.regex.pattern
        vcsTrackedOnlyCheckbox.box.isSelected = custom.onlyVcsTracked

        initialSettings = this.getStored()
    }

    override fun modified(): Boolean {
        return !utils.isEqual(initialSettings, this.getStored())
    }

    override fun verify(): String {
        var res = verifyActionName(actionNameField.text())
        if (res.isNotEmpty()) return res

        res = verifyRegex(regexPatternField.text())
        if (res.isNotEmpty()) return res

        if (pathField.text().isEmpty()) {
            return "Please provide a path"
        }

        val shortcut = shortcutField.text().trim()
        return verifyShortcut(shortcut)
    }

    override fun getStored(): Array<String> {
        return arrayOf(
            ActionType.SEARCH_FILE_MATCHING_PATTERN.toString(),
            actionNameField.text(),
            shortcutField.text(),
            pathField.text(),
            regexPatternField.text(),
            vcsTrackedOnlyCheckbox.box.isSelected.toString())
    }

    override fun help(): String {
        return """
            <b>Open a popup to search for through files matching the given regex pattern</b><br>
            
            Search through all files in the specified path and match files where the filename (with extension) matches the regex.
            The resulting list is searched through, allowing to more quickly find specific types of files (e.g. finding all test files).
            Matches on the filename only (with extension).<br>
            <b>Name:</b> Enter a unique name, this is used to register the action in the intellij framework. The action will be registered as <i>com.fuzzyfilesearch.%NAME%</i><br>
            <b>Path:</b> Enter a path, where '/' is the project root. To search relative to the open file, start with './'<br>
            <b>Regex:</b> Enter a valid regex to match files, e.g. find all files ending with <i>Test</i><br>
            <b>Shortcut (optional):</b> Enter a shortcut to trigger the action <br>
        """.trimIndent()
    }
}
