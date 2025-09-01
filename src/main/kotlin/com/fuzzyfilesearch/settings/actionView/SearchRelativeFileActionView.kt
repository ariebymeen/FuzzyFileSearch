package com.fuzzyfilesearch.settings.actionView

import com.fuzzyfilesearch.actions.ActionType
import com.fuzzyfilesearch.actions.SearchRelativeFileAction
import com.fuzzyfilesearch.actions.utils
import com.fuzzyfilesearch.components.LabeledTextField
import com.fuzzyfilesearch.components.WrappedCheckbox
import com.fuzzyfilesearch.settings.verifyActionName
import com.fuzzyfilesearch.settings.verifyRegex
import com.fuzzyfilesearch.settings.verifyShortcut
import javax.swing.JPanel

class SearchRelativeFileActionView() : ActionViewBase() {
    val regexPatternField =
            LabeledTextField(
                "Regex: ",
                "Enter a regex to find a reference file with a matching name. (e.g. 'CMakeLists.txt' finds the closes cmake file to the current file)")
    val extensionFilterField =
            LabeledTextField(
                "Extensions filter: ",
                "Enter a comma seperated list of extensions to filter (e.g. '.kt,.java')")
    val vcsTrackedOnlyCheckbox = WrappedCheckbox(
        "Only search files tracked by vcs",
        "If selected, only search files that are tracked by vcs")

    override fun addToPanel(panel: JPanel) {
        panel.add(actionNameField)
        panel.add(regexPatternField)
        panel.add(extensionFilterField)
        panel.add(shortcutField)
        panel.add(vcsTrackedOnlyCheckbox)
    }

    override fun initialize(settings: utils.ActionSettings) {
        actionNameField.textField.text = settings.name
        shortcutField.textField.text = settings.shortcut

        val custom = SearchRelativeFileAction.parseSettings(settings.generic)
        regexPatternField.textField.text = custom.regex.pattern
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
            ActionType.SEARCH_FILE_IN_RELATED_PATH.toString(),
            actionNameField.text(),
            shortcutField.text(),
            regexPatternField.text(),
            extensionFilterField.text(),
            vcsTrackedOnlyCheckbox.box.isSelected.toString())
    }

    override fun help(): String {
        return """
            <b>Search through files that are located in the same or a subdirectory of a marker file</b><br>
            Use this action if your projects have a predictable structure, like having a pom or cmake file. 
            This action can find this file and search from there, allowing to search within some module in your project. <br>
            
            <b>Name:</b> Enter a unique name, this is used to register the action in the intellij framework. The action will be registered as <i>com.fuzzyfilesearch.%NAME%</i><br>
            <b>Regex:</b> Regex to find the marker file, the most closely located file that matches this pattern is selected as the search location<br>
            <b>Extensions filter (optional):</b> List the extensions to search over, seperated by ','. If empty, all file extensions in the (recursive) directory are included in the search. <br>
            <b>Shortcut (optional):</b> Enter a shortcut to trigger the action <br>
        """.trimIndent()
    }
}
