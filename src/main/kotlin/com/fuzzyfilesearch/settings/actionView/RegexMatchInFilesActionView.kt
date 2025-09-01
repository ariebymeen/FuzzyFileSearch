package com.fuzzyfilesearch.settings.actionView

import com.fuzzyfilesearch.actions.ActionType
import com.fuzzyfilesearch.actions.RegexMatchInFiles
import com.fuzzyfilesearch.actions.utils
import com.fuzzyfilesearch.components.LabeledTextField
import com.fuzzyfilesearch.components.WrappedCheckbox
import com.fuzzyfilesearch.settings.verifyActionName
import com.fuzzyfilesearch.settings.verifyRegex
import com.fuzzyfilesearch.settings.verifyShortcut
import com.intellij.ui.components.JBTextArea
import javax.swing.JPanel

class RegexMatchInFilesActionView() : ActionViewBase() {

    val pathField = LabeledTextField(
        "Path: ",
        "Enter a path. Starting with '/' searches from the project root, starting with '.' searches from the current file. Leaving it empty searches only the current file",
        "/")
    val regexPatternField =
            LabeledTextField(
                "Regex: ",
                "Regex pattern to find matches in the file contents.")
    val extensionFilterField =
            LabeledTextField(
                "Extensions filter: ",
                "Enter a comma seperated list of extensions to filter (e.g. '.kt,.java')")
    val vcsTrackedOnlyCheckbox = WrappedCheckbox(
        "Only search files tracked by vcs",
        "If selected, only search files that are tracked by vcs")

    override fun addToPanel(panel: JPanel) {
        panel.add(actionNameField)
        panel.add(pathField)
        panel.add(regexPatternField)
        panel.add(extensionFilterField)
        panel.add(shortcutField)
        panel.add(vcsTrackedOnlyCheckbox)
    }

    override fun initialize(settings: utils.ActionSettings) {
        actionNameField.textField.text = settings.name
        shortcutField.textField.text = settings.shortcut

        val custom = RegexMatchInFiles.parseSettings(settings.generic)
        regexPatternField.textField.text = custom.regex.pattern
        pathField.textField.text = custom.path
        extensionFilterField.textField.text =
                if (custom.extensionList.isEmpty()) "" else custom.extensionList.joinToString(", ")
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

        val shortcut = shortcutField.text().trim()
        return verifyShortcut(shortcut)
    }

    override fun getStored(): Array<String> {
        return arrayOf(
            ActionType.REGEX_SEARCH_IN_FILES.toString(),
            actionNameField.text(),
            shortcutField.text(),
            regexPatternField.text(),
            pathField.text(),
            extensionFilterField.text(),
            vcsTrackedOnlyCheckbox.box.isSelected.toString())
    }

    override fun help(): String {
        return """
            <b>Opens a search view to search through all lines that match the specified regex pattern</b><br>
            Will first search for all files in the specified path, for files (tracked by vcs if checked) with the specified extension.
            Will find all regex matches in all files that are found. The resulting matches can be search though. 
            Can be found for example to search through all functions in the current file or search for all classes in all .kt files
            
            <b>Name:</b> Enter a unique name, this is used to register the action in the intellij framework. The action will be registered as <i>com.fuzzyfilesearch.%NAME%</i><br>
            <b>Path (optional):</b> Enter a path, where '/' is the project root. To search relative to the open file, start with './'. If empty, only search through the current file<br>
            <b>Regex:</b> Regex pattern to find all matches in the file contents<br>
            <b>Extensions filter (optional):</b> List the extensions to search through, seperated by ','. If empty, all files in the (recursive) directory are included in the search. <br>
            <b>Shortcut (optional):</b> Enter a shortcut to trigger the action <br>
        """.trimIndent()
    }
}
