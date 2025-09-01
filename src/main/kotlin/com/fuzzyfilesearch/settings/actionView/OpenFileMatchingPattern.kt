package com.fuzzyfilesearch.settings.actionView

import com.fuzzyfilesearch.actions.ActionType
import com.fuzzyfilesearch.actions.OpenFileMatchingPattern
import com.fuzzyfilesearch.actions.utils
import com.fuzzyfilesearch.settings.verifyActionName
import com.fuzzyfilesearch.settings.verifyShortcut
import com.intellij.ui.components.JBTextField
import java.awt.GridLayout
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class PatternEntryFields() {
    val actionNameField = JBTextField().apply {
        toolTipText = "Action name"
    }
    val patternField = JBTextField().apply {
        toolTipText = "Pattern"
    }
    val shortcutField = JBTextField().apply {
        toolTipText = "Shortcut"
    }

    val wrapper = JPanel().apply {
        layout = GridLayout(1, 3, 5, 5)
        add(actionNameField)
        add(patternField)
        add(shortcutField)
    }
}

class OpenFileMatchingPattern() : ActionViewBase() {
    val actionsPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }
    val actions: MutableList<PatternEntryFields> = mutableListOf(PatternEntryFields(), PatternEntryFields())
    val newButton = JButton("Add pattern").apply {
        toolTipText = "Add pattern"
    }
    init {
        newButton.addActionListener {
            actions.add(PatternEntryFields())
            actionsPanel.removeAll()
            actions.forEach { actionsPanel.add(it.wrapper) }
            actionsPanel.revalidate()
            actionsPanel.repaint()
        }
    }

    override fun addToPanel(panel: JPanel) {
        val title = JPanel().apply {
            layout = GridLayout(1, 3, 5, 3)
            add(JLabel("Action name:"))
            add(JLabel("Pattern:"))
            add(JLabel("Shortcut (optional):"))
        }
        panel.add(title)
        actions.forEach { actionsPanel.add(it.wrapper) }
        panel.add(actionsPanel)
        val buttonWrapper = JPanel().apply {
            add(newButton)
        }
        panel.add(buttonWrapper)
    }

    override fun initialize(settings: utils.ActionSettings) {
        actions.clear()

        val custom = OpenFileMatchingPattern.parseSettings(settings.generic)
        custom.forEach {
            val entry = PatternEntryFields()
            entry.actionNameField.text = it.name
            entry.patternField.text    = it.settings.pattern
            entry.shortcutField.text   = it.shortcut
            actions.add(entry)
        }

        initialSettings = this.getStored()
    }

    override fun modified(): Boolean {
        return !utils.isEqual(initialSettings, this.getStored())
    }

    override fun verify(): String {
        if (actions.all { it.actionNameField.text.isEmpty() }) {
            return "Please provide a (unique) action name"
        }

        actions.forEach{ action ->
            if (action.actionNameField.text.isNotEmpty()) {
                val res = verifyActionName(action.actionNameField.text)
                if (res.isNotEmpty()) {
                    return res
                }
                val resShortcut = verifyShortcut(action.shortcutField.text)
                if (resShortcut.isNotEmpty()) {
                    return resShortcut
                }
            } else {
                if (action.patternField.text.isNotEmpty()) {
                    return "Please provide a (unique) action name if the pattern is not empty"
                }
                if (action.shortcutField.text.isNotEmpty()) {
                    return "Please provide a (unique) action name if the shortcut is not empty"
                }
            }
        }
        return ""
    }

    override fun getStored(): Array<String> {
        val actionsList = actions.filter { it.actionNameField.text.isNotEmpty() }
                                 .flatMap { listOf(it.actionNameField.text, it.patternField.text, it.shortcutField.text) }
        return arrayOf(
            ActionType.OPEN_FILE_MATCHING_PATTERN.toString(),
            "", // The action name for other types, here we have multiple action names
            "", // The shortcut for other types, here we have multiple shortcuts
            ) + actionsList.toTypedArray()
    }

    override fun getActionNames(): List<String> {
        return actions.map { it.actionNameField.text }.filter { it.isNotEmpty() }
    }

    override fun getShortcuts(): List<String> {
        return actions.map { it.shortcutField.text }.filter { it.isNotEmpty() }
    }

    override fun help(): String {
        return """
            <b>Quickly jump between files with the given patterns</b><br>
            Use this action to quickly jump between project files, e.g. quickly switch between a implementation and test file.<br>
            Define multiple patterns, for example:
            <ul>
                <li>%name%.cc --> Jump to the source file</li>
                <li>%name%.h  --> Jump to the header file</li>
                <li>%name%Test.cc --> Jump to the test file</li>
                <li>%name%Builder.cc --> Jump to the builder file</li>
            </ul>
            When in the test file, jumping to the header file, %name% is resolved from the currently open file name.
            The resolved %name% is then replaced in the target pattern, and the file with this name is opened, given the file is located somewhere in the project root.
            
            <b>Action name:</b> Enter a unique name, this is used to register the action in the intellij framework. The action will be registered as <i>com.fuzzyfilesearch.%NAME%</i><br>
            <b>Pattern: </b> Enter a pattern, optionally container <li>%name%</li>, as this is resolved using the currently open file name. Defines the target file to open<br>
            <b>Shortcut (optional):</b> Enter a shortcut to trigger the action (switch to the file satisfying this pattern) <br>
        """.trimIndent()
    }
}