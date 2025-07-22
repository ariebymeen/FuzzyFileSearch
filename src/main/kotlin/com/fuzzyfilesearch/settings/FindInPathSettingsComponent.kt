package com.fuzzyfilesearch.settings

import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.KeyStroke
import kotlin.text.startsWith

enum class ActionType {
    SEARCH_FILE_IN_PATH,
    SEARCH_FILE_IN_RELATIVE_PATH,
    SEARCH_FILE_MATCHING_PATTERN,
    SEARCH_RECENT_FILES,
    SEARCH_OPEN_FILES,
    OPEN_FILE_MATCHING_PATTERN,
}

class LabeledTextField(labelText: String) : JPanel(BorderLayout()) {
    val textField = JTextField() // TODO: Set min width?
    init {
        add(JLabel(labelText), BorderLayout.WEST)
        add(textField, BorderLayout.CENTER)
    }

    fun text(): String {
        return textField.text
    }
}

fun verifyShortcut(shortcut: String): String {
    if (shortcut.isNotEmpty()) {
        val keyStroke = KeyStroke.getKeyStroke(shortcut)
        if (keyStroke == null) {
            return "Shortcut '$shortcut' is not a valid shortcut. Please provide something similar to 'alt shift U'"
        }
        val sc = KeyboardShortcut(keyStroke, null)
        val actions = KeymapManager.getInstance().activeKeymap.getActionIdList(sc)
        val error = actions.any { !it.startsWith("com.fuzzyfilesearch.") }
        if (error) {
            return "Error, shortcut $shortcut is already in use with $actions. Go to keymap settings to remove this shortcut"
        }
    }
    return ""
}

abstract class ActionComponentBase() {
    // Keep track of your own stored set as the mSettings array also contains elements not of your set
    var storedSet: Array<String> = emptyArray()
    val actionNameField         = LabeledTextField("Name: ")
    val shortcutField           = LabeledTextField("Shortcut: ")

    /** Add interface items to panel */
    abstract fun addToPanel  (panel: JPanel)
    /** Add interface items to panel, but from the settings */
    abstract fun initialize  (panel: JPanel, settings: Array<String>)
    abstract fun modified    (): Boolean
    abstract fun verify      (): String

    fun getStored(): Array<String> {
        return storedSet
    }
    fun getActionName(): String {
        return actionNameField.text()
    }
    fun getShortcut(): String {
        return shortcutField.text()
    }
}

class FindInPathComponent() : ActionComponentBase() {
    val pathField               = LabeledTextField("Path: ")
    val extensionFilterField    = LabeledTextField("Extensions filter: ")

    override fun addToPanel(panel: JPanel) {
        panel.add(actionNameField)
        panel.add(pathField)
        panel.add(extensionFilterField)
        panel.add(shortcutField)
    }

    override fun initialize(panel: JPanel, settings: Array<String>) {
        // Settings: [name, path, extensions, shortcut]
        actionNameField.textField.text      = settings.getOrElse(0) { "DefaultName" }
        pathField.textField.text            = settings.getOrElse(1) { "/" }
        extensionFilterField.textField.text = settings.getOrElse(2) { "" }
        shortcutField.textField.text        = settings.getOrElse(3) { "" }
        storedSet = arrayOf(actionNameField.text(), pathField.text(), extensionFilterField.text(), shortcutField.text())
        this.addToPanel(panel)
    }

    override fun modified(): Boolean {
        return actionNameField.text()       != storedSet.getOrNull(0) ||
               pathField.text()             != storedSet.getOrNull(1) ||
               extensionFilterField.text()  != storedSet.getOrNull(2) ||
               shortcutField.text()         != storedSet.getOrNull(3)
    }

    override fun verify(): String {
        if (actionNameField.text().isEmpty()) {
            return "Please provide a (unique) name"
        }

        if (actionNameField.text().trim().contains(' ')) {
            return "Please provide a name without spaces"
        }

        if (pathField.text().isEmpty()) {
            return "Please provide a path"
        }

        val shortcut = shortcutField.text().trim()
        return verifyShortcut(shortcut)
    }
}

class FindInPathSettingsComponent(val mParent: JPanel): JPanel(BorderLayout()) {
    val combobox = ComboBox(ActionType.values())
    val warningLabel = JBLabel().apply {
        this.foreground = JBColor.RED
        this.isVisible = false
        this.border = JBUI.Borders.empty()
    }
    val contents = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }
    val removeButton = JButton("Delete").apply {
        addActionListener {
            mParent.remove(combobox.parent)
            mParent.revalidate()
            mParent.repaint()
        }
    }
    val rbPanel = JPanel(BorderLayout()).apply {
        border = JBUI.Borders.customLine(JBColor.LIGHT_GRAY, 0, 0, 1, 0)
        add(removeButton, BorderLayout.WEST)
    }

    var item: ActionComponentBase? = null

    init {
        add(combobox, BorderLayout.NORTH)
        add(contents, BorderLayout.CENTER)
    }

    fun initializeDefault() {
        combobox.selectedItem = ActionType.SEARCH_FILE_IN_PATH
        setItemBasedOnSelectedType()
        combobox.addItemListener{ e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                setItemBasedOnSelectedType()
                fillContentBox()
            }
        }

        fillContentBox()
    }

    fun initializeFromSettings(settings: Array<String>, type: ActionType, index: Int) {
        combobox.isEnabled = false
        combobox.selectedItem = type
        setItemBasedOnSelectedType()
        item?.initialize(contents, settings)
        fillContentBox()
    }

    fun getType(): ActionType {
        return combobox.selectedItem as ActionType
    }

    fun getStored(): Array<String> {
        return item!!.getStored()
    }

    fun isModified(): Boolean {
        if (item == null) return false
        val error = item!!.verify()
        if (error.isNotEmpty()) {
            warningLabel.text = error
            warningLabel.isVisible = true
            return false
        } else {
            warningLabel.isVisible = false
        }

        return item!!.modified()
    }

    fun fillContentBox() {
        contents.removeAll()

        val panel = JPanel(BorderLayout())
        panel.add(warningLabel)
        contents.add(panel)
        item?.addToPanel(contents)
        contents.add(rbPanel)

        contents.revalidate()
        contents.repaint()
    }

    fun setItemBasedOnSelectedType() {
        val selected = combobox.selectedItem as ActionType
        when (selected) {
            ActionType.SEARCH_FILE_IN_PATH -> {
                item = FindInPathComponent()
            }
            ActionType.SEARCH_FILE_IN_RELATIVE_PATH-> {
                contents.add(JTextField("Find file in relative path action"))
            }
            ActionType.SEARCH_FILE_MATCHING_PATTERN-> {

            }
            ActionType.OPEN_FILE_MATCHING_PATTERN-> {

            }
            ActionType.SEARCH_RECENT_FILES -> {

            }
            ActionType.SEARCH_OPEN_FILES -> {

            }
        }
    }
}