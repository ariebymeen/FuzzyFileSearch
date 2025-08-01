package com.fuzzyfilesearch.settings.actionView

import com.fuzzyfilesearch.actions.ActionType
import com.fuzzyfilesearch.actions.OpenRelativeFileAction
import com.fuzzyfilesearch.actions.utils
import com.fuzzyfilesearch.components.LabeledTextField
import com.fuzzyfilesearch.settings.verifyActionName
import com.fuzzyfilesearch.settings.verifyRegex
import com.fuzzyfilesearch.settings.verifyShortcut
import javax.swing.JPanel

class OpenRelativeFileActionView() : ActionViewBase() {
    val regexPatternField =
            LabeledTextField(
                "Regex: ",
                "Enter a regex to find the closes file where the name matches the regex pattern.")
    val filePatternField =
            LabeledTextField(
                "File pattern: ",
                "Specify a pattern to open the file (starting from the matched file directory)")

    override fun addToPanel(panel: JPanel) {
        panel.add(actionNameField)
        panel.add(regexPatternField)
        panel.add(filePatternField)
        panel.add(shortcutField)
    }

    override fun initialize(settings: utils.ActionSettings) {
        actionNameField.textField.text = settings.name
        shortcutField.textField.text = settings.shortcut

        val custom = OpenRelativeFileAction.parseSettings(settings.generic)
        regexPatternField.textField.text = custom.regex.pattern
        filePatternField.textField.text =
                if (custom.filePatterns.isEmpty()) "" else custom.filePatterns.joinToString(" | ")

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

        if (filePatternField.text().isEmpty()) {
            return "Please provide a file pattern to open the file"
        }

        val shortcut = shortcutField.text().trim()
        return verifyShortcut(shortcut)
    }

    override fun getStored(): Array<String> {
        return arrayOf(
            ActionType.OPEN_RELATIVE_FILE.toString(),
            actionNameField.text(),
            shortcutField.text(),
            regexPatternField.text(),
            filePatternField.text())
    }

    override fun help(): String {
        return """
                Open a file that is related to the currently open file. If no regex is entered, %name% is set to the name of the current file (without extension).
                If not empty, %rname% is set to the name of the file that matches the regex that is closest to the currently open file (without extension).
                The action to open the file starts from the reference file directory, so enter a relative path.
                The %cname% variable is set to the name of the currently open file. This name is compared with the files in the open path.
                If a file in the directory matches partly, it is considered to be the same
                (if the current filename is MyFileTest it will open the file MyFile unless MyFileTest also exists in the open path).
                Note that %rname% and %cname% cannot be used at the same time. If you want to have multiple options use the | to split them. The options
                are evaluated in order""${'"'}
        """.trimIndent()
    }
}
