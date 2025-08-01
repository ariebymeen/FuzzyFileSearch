package com.fuzzyfilesearch.settings.actionView

import com.fuzzyfilesearch.actions.ActionType
import com.fuzzyfilesearch.actions.utils
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

class ActionViewWrapper(
    val mParent: JPanel,
    types: Array<ActionType>) : JPanel(BorderLayout()) {
    val container = JPanel(BorderLayout()) // container inside jpanel to create border padding
    val combobox = ComboBox(types)
    val cbPanel = JPanel(BorderLayout()).apply {
        add(combobox)
    }
    val warningLabel = JBLabel().apply {
        this.foreground = JBColor.RED
        this.isVisible = false
        this.border = JBUI.Borders.empty()
    }
    val contents = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }
    val removeButton = JButton("Remove").apply {
        addActionListener {
            mParent.remove(container.parent)
            mParent.revalidate()
            mParent.repaint()
        }
        toolTipText = "Remove this action from settings"
    }
    val helpButton = JBLabel(AllIcons.General.ContextHelp)
    val rbPanel = JPanel(BorderLayout()).apply {
        border = JBUI.Borders.emptyBottom(2)
        add(removeButton, BorderLayout.WEST)
        add(helpButton, BorderLayout.EAST)
    }

    var item: ActionViewBase? = null

    init {
        add(container)
        container.add(cbPanel, BorderLayout.NORTH)
        container.add(contents, BorderLayout.CENTER)

        border = JBUI.Borders.empty(4)
        container.border = JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.LIGHT_GRAY, 0, 2, 0, 0),
            JBUI.Borders.emptyLeft(10),
            JBUI.Borders.emptyBottom(2))
    }

    fun initializeDefault() {
        combobox.selectedItem = ActionType.SEARCH_FILE_IN_PATH
        setItemBasedOnSelectedType()
        combobox.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                setItemBasedOnSelectedType()
                fillContentBox()
            }
        }

        fillContentBox()
    }

    fun initializeFromSettings(settings: utils.ActionSettings) {
        combobox.isEnabled = false
        combobox.selectedItem = settings.type
        setItemBasedOnSelectedType()
        item?.initialize(settings)
        fillContentBox()
    }

    fun getType(): ActionType {
        return combobox.selectedItem as ActionType
    }

    fun getStored(): Array<String> {
        return item!!.getStored()
    }

    fun getShortcut(): String {
        return item!!.getShortcut()
    }

    fun getActionName(): String {
        return item!!.getActionName()
    }

    fun setWarning(text: String) {
        warningLabel.text = text
        warningLabel.isVisible = true
    }

    fun isModified(): Boolean {
        if (item == null) return false
        val error = item!!.verify()
        if (error.isNotEmpty()) {
            this.setWarning(error)
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
        val contentsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.emptyLeft(4)
        }
        item?.addToPanel(contentsPanel)
        contents.add(contentsPanel)
        contents.add(rbPanel)

        contents.revalidate()
        contents.repaint()
    }

    fun setItemBasedOnSelectedType() {
        val selected = combobox.selectedItem as ActionType
        item = when (selected) {
            ActionType.SEARCH_FILE_IN_PATH          -> SearchFileInPathActionView()
            ActionType.SEARCH_FILE_IN_RELATED_PATH  -> SearchRelativeFileActionView()
            ActionType.SEARCH_FILE_MATCHING_PATTERN -> SearchFilesWithPatternActionView()
            ActionType.OPEN_RELATIVE_FILE           -> OpenRelativeFileActionView()
            ActionType.SEARCH_RECENT_FILES          -> SearchRecentFilesActionView()
            ActionType.SEARCH_OPEN_FILES            -> SearchOpenFilesActionView()
            ActionType.GREP_IN_FILES                -> GrepInFilesActionView()
            ActionType.REGEX_SEARCH_IN_FILES        -> RegexMatchInFilesActionView()
        }
        helpButton.toolTipText = if (item == null) "" else item!!.help()
    }
}