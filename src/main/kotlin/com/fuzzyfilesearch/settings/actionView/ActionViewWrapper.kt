package com.fuzzyfilesearch.settings.actionView

import com.fuzzyfilesearch.actions.ActionType
import com.intellij.ui.components.JBTextArea
import com.fuzzyfilesearch.actions.utils
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JEditorPane
import com.intellij.openapi.util.IconLoader
import javax.swing.JPanel

class MyEditorPane(type: String, text: String?) : JEditorPane(type, text) {
    override fun getPreferredSize(): Dimension {
        val d = super.getPreferredSize()
        val parentWidth = parent?.width ?: d.width
        // Restrict to parent’s width
        return Dimension(parentWidth, d.height)
    }
}

class ActionViewWrapper(
    val mParent: JPanel,
    types: Array<ActionType>) : JPanel(BorderLayout()) {
    val container = JPanel(BorderLayout()) // container inside jpanel to create border padding
    val combobox = ComboBox(types)
    val cbPanel = JPanel(BorderLayout()).apply {
        add(combobox)
    }
    val warningLabel = JBTextArea().apply {
        foreground = JBColor.RED
        isVisible = false
        isEditable = false
        wrapStyleWord = true
        background = null
        isOpaque = false
        border = JBUI.Borders.empty()
        font = JBLabel().font
    }
    val infoLabel = MyEditorPane("text/html", "").apply {
        isEditable = false
        isVisible = false
        background = null
        isOpaque = false
        border = JBUI.Borders.empty()
        font = JBLabel().font
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
    val helpButton = JButton(AllIcons.General.ContextHelp).apply {
        border = JBUI.Borders.empty()
        preferredSize = Dimension(AllIcons.General.ContextHelp.iconWidth, height)
    }
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

        helpButton.addActionListener {
            infoLabel.isVisible = infoLabel.isVisible.not()
            val text = if (item == null) "" else item!!.help()
            val html = $$"""
            <html>
            <body style="width:100%; font-family: ${JBFont.label().family}; font-size:${JBFont.label().size}pt;">
            $$text
            </body>
            </html>
            """.trimIndent()
            infoLabel.text = html
        }
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

//    fun getType(): ActionType {
//        return combobox.selectedItem as ActionType
//    }

    fun getStored(): Array<String> {
        return item!!.getStored()
    }

    fun getShortcuts(): List<String> {
        return item!!.getShortcuts()
    }

    fun getActionNames(): List<String> {
        return item!!.getActionNames()
    }

    fun setWarning(text: String) {
        println("Setting warning text: $text")
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

        val warningPanel = JPanel(BorderLayout())
        warningPanel.add(warningLabel)
        contents.add(warningPanel)
        val infoPanel = JPanel(BorderLayout())
        infoPanel.add(infoLabel)
        contents.add(infoPanel)
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
            ActionType.OPEN_FILE_MATCHING_PATTERN   -> OpenFileMatchingPattern()
            ActionType.SEARCH_RECENT_FILES          -> SearchRecentFilesActionView()
            ActionType.SEARCH_OPEN_FILES            -> SearchOpenFilesActionView()
            ActionType.GREP_IN_FILES                -> GrepInFilesActionView()
            ActionType.REGEX_SEARCH_IN_FILES        -> RegexMatchInFilesActionView()
        }
        helpButton.toolTipText = if (item == null) "" else item!!.help()
    }
}