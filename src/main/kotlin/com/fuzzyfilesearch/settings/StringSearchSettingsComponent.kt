package com.fuzzyfilesearch.settings

import com.fuzzyfilesearch.actions.ActionType
import com.fuzzyfilesearch.settings.actionView.ActionViewWrapper
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.invokeLater
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

class StringSearchSettingsComponent(val mSettings: GlobalSettings.SettingsState) {
    var panel: JPanel
    var keeper = SettingsComponentKeeper()
    val exportButton = JButton("Export actions").apply {
        addActionListener {
            exportActionsToFile(mSettings, actionTypes)
        }
    }
    val importButton = JButton("Import actions").apply {
        addActionListener {
            importFromFile(mSettings)
            refreshActionsPanel()
        }
    }
    val actionsCollectionPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }
    val actionTypes = ActionType.values().takeLast(3).toTypedArray()
    val rbPanel = JPanel(BorderLayout()).apply {
        border = JBUI.Borders.empty()
        val newButton = JButton("New").apply {
            addActionListener {
                val comp = ActionViewWrapper(
                    actionsCollectionPanel,
                    actionTypes)
                comp.initializeDefault()
                actionsCollectionPanel.add(comp)
                actionsCollectionPanel.revalidate()
                actionsCollectionPanel.repaint()
            }
        }
        add(newButton, BorderLayout.CENTER)
    }

    init {
        val builder = FormBuilder()
            .addComponent(JBLabel("<html><strong>Popup settings for string search</strong></html>"))
            .addSeparator()

        keeper.createJBIntSpinnerComponent(
            mSettings.string::numberOfFilesInSearchView, 10, 1, 100, 1, builder,
            "Max number of files visible in search view", """
            Sets the maximum number of files in the search view. Defaults to 10, as this is the most that can be selected
            by number. Setting this value too high can seriously affect the rendering performance. Not that this only affects
            the number of files visible, all other files are still used in searching""".trimIndent())
        val policyCombobox = keeper.createComboboxComponent(
            mSettings.string::popupSizePolicy, PopupSizePolicy.values(), builder, "Popup scaling", """
               Select how the popup resizes. Fixed size will allow you to specify the size in pixels. Resize with
               ide bounds: specify the size of the popup as a fraction of the ide size. Resize with screen size: 
               specify the size of the popup as a fraction of the screen size. 
               This may give unexpected behaviour on multi-monitor setups""".trimIndent())
        val searchPopupWidthSpinner = keeper.createJSpinnerComponent(
            mSettings.string::searchPopupWidth,
            0.3,
            0.1,
            1.0,
            0.05,
            builder,
            "Search view width fraction",
            """The width of the search popup as a fraction of the screen width """.trimIndent())
        val searchPopupHeightSpinner = keeper.createJSpinnerComponent(
            mSettings.string::searchPopupHeight,
            0.3,
            0.1,
            1.0,
            0.05,
            builder,
            "Search view height fraction",
            """The width of the search popup as a fraction of the screen height""".trimIndent())
        val searchPopupWidthPxSpinner = keeper.createJBIntSpinnerComponent(
            mSettings.string::searchPopupWidthPx, 700, 50, 10000, 5, builder,
            "Fixed width of popup in pixels",
            """ Use a fixed size popup. This sets the width of the popup in pixels irrespective of the screen or ide size""".trimIndent())
        val searchPopupHeightPxSpinner = keeper.createJBIntSpinnerComponent(
            mSettings.string::searchPopupHeightPx, 700, 50, 10000, 5, builder,
            "Fixed height of popup in pixels", """
                    Use a fixed size popup. This sets the height of the popup in pixels irrespective of the screen or ide size""".trimIndent())
        keeper.createJSpinnerComponent(
            mSettings.string::horizontalPositionOnScreen,
            0.5,
            0.1,
            1.0,
            0.01,
            builder,
            "X Position of search area on screen",
            """Relative X position on screen. 0 means all the way left, 1 means all the way right""".trimIndent())
        keeper.createJSpinnerComponent(
            mSettings.string::verticalPositionOnScreen,
            0.5,
            0.1,
            1.0,
            0.01,
            builder,
            "Y Position of search area on screen",
            """Relative Y position on screen. 0 means all the way left, 1 means all the way right""".trimIndent())
        keeper.createJBIntSpinnerComponent(
            mSettings.string::searchBarHeight, 30, 10, 300, 1, builder,
            "Height of the search bar in pixels", """""".trimIndent())
        keeper.createJBIntSpinnerComponent(
            mSettings.string::searchItemHeight, 30, 10, 100, 1, builder,
            "Height of the search items in pixels", """""".trimIndent())
        keeper.createCheckboxComponent(
            mSettings.string::showFileIcon, builder, "Show file icon",
            """""".trimIndent())
        keeper.createCheckboxComponent(
            mSettings.string::showNumberInSearchView, builder, "Show the index of each item in search view",
            """If checked show the number (index) of the item in the view as a number in front of the result """.trimIndent())
        val shrinkViewCheckbox = keeper.createCheckboxComponent(
            mSettings.string::shrinkViewDynamically, builder, "Shrink the search area to only the found results",
            """If checked the search area will shrink to the number of results. Else the search area height
                will always be the configured height""".trimIndent())
        val showEditorPreviewCheckbox = keeper.createCheckboxComponent(
            mSettings.string::showEditorPreview, builder, "Show editor preview",
            """ If checked, a small editor will be shown with the selected file contents. Can be used to quickly 
                    edit files. May negatively impact the performance. If selected, shrinking the search box is not supported""".trimIndent())
        val editorLocation = keeper.createComboboxComponent(
            mSettings.string::editorPreviewLocation, EditorLocation.values(), builder, "Location of the editor preview",
            """Show the preview editor either below or to the right of the search box""".trimIndent())
        val editorMinSize = keeper.createJBIntSpinnerComponent(
            mSettings.string::minSizeEditorPx, 200, 40, 5000, 5, builder,
            "Min size of the editor view in pixels", """Minimum size of the editor in pixels. 
                    If the popup size is scaled with the size of the editor and the editor size is below this value, the editor is hidden. """.trimIndent())
        val editorSize = keeper.createJSpinnerComponent(
            mSettings.string::editorSizeRatio, 0.5, 0.1, 1.0, 0.01, builder, "Editor preview ratio", """
                    The ratio of the preview editor size as a fraction of the total width or height of the popup. 
                    If the preview editor is shown below the search area, the fraction of the total height will be selected.
                    If the preview editor is shown to the right of the search area, the fraction of the total width will be selected""".trimIndent())

        builder.addComponent(JBLabel("<html><strong>Common settings</strong></html>"))
               .addSeparator()
        keeper.createCheckboxComponent(
            mSettings::applySyntaxHighlightingOnTextSearch, builder, "Apply syntax highlighting on text search", """
                    If checked, apply syntax highlighting on text search results. If false, plain text is used""".trimIndent())
        keeper.createCheckboxComponent(
            mSettings::showLineNumberWithFileName, builder, "Show the line number of the match", """
                If checked, show the filename:line number of the matching line """.trimIndent())
        if (mSettings.common.enableDebugOptions) {
            keeper.createCheckboxComponent(
                mSettings.string::searchMultiThreaded,
                builder,
                "Testoption, search multithreaded",
                "")
        }

        // Create Relative file opening actions
        builder.addComponent(JBLabel("<html><strong>Regex search settings</strong></html>"))
            .addSeparator()
        keeper.createComboboxComponent(
            mSettings::showFilenameForRegexMatch, ShowFilenamePolicy.values(), builder,
            "Show filename in regex search", """
                    Show filename in the result list""".trimIndent())

        builder.addComponent(JBLabel("<html><strong>Code element search settings</strong></html>"))
            .addSeparator()
        keeper.createComboboxComponent(
            mSettings::showFilenameForPsiSearch, ShowFilenamePolicy.values(), builder,
            "Show filename in code element search", """
                    Show filename in the result list""".trimIndent())

        builder.addComponent(JBLabel("<html><strong>Live grep settings</strong></html>"))
            .addSeparator()
        keeper.createCheckboxComponent(
            mSettings::useSelectedTextForGrepInFiles, builder, "Use selected text as initial query", """
                    If true, the selected text is used as initial query. If no text is selected, no query is set """.trimIndent())
        keeper.createCheckboxComponent(
            mSettings::showFilenameForGrepInFiles, builder, "Show filename in grep result", """
                    Show filename in the result list""".trimIndent())
        keeper.createJBIntSpinnerComponent(
            mSettings::grepRememberPreviousQuerySeconds, 5, 0, 100, 1, builder, "Remember previous query (seconds)", """
                    Time (seconds) for which the previous query will be remembered. When zero, it is never remembered""".trimIndent())
        keeper.createJBIntSpinnerComponent(
            mSettings::minNofLinesBetweenGrepResults, 2, 0, 100, 1, builder, "Min lines between results", """
                    Minimum number of lines between live grep results. Prevents having many matches that are very closely placed""".trimIndent())

        builder.addComponent(JBLabel("<html><strong>Actions</strong></html>"))
               .addSeparator()
               .addComponent(actionsCollectionPanel)
               .addComponent(rbPanel)
               .addSeparator()

        refreshActionsPanel()
        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(exportButton)
            add(importButton)
        }
        builder.addComponent(buttonPanel)
            .addComponentFillVertically(JPanel(), 0)

        panel = builder.panel

        fun setEnabledDisabled() {
            invokeLater {
                val isFixed = policyCombobox.selectedItem == PopupSizePolicy.FIXED_SIZE
                searchPopupWidthSpinner.isEnabled = !isFixed
                searchPopupHeightSpinner.isEnabled = !isFixed
                searchPopupWidthPxSpinner.isEnabled = isFixed
                searchPopupHeightPxSpinner.isEnabled = isFixed

                val editorEnabled = showEditorPreviewCheckbox.isSelected
                editorSize.isEnabled = editorEnabled
                editorLocation.isEnabled = editorEnabled
                editorMinSize.isEnabled = editorEnabled
                shrinkViewCheckbox.isEnabled = !editorEnabled
            }
        }

        policyCombobox.addItemListener{ e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                setEnabledDisabled()
            }
        }
        showEditorPreviewCheckbox.addActionListener { e ->
            setEnabledDisabled()
        }
    }

    fun refreshActionsPanel() {
        actionsCollectionPanel.removeAll()
        addActionConfigsToPanel(mSettings.allActions, actionsCollectionPanel, actionTypes)
        actionsCollectionPanel.revalidate()
        actionsCollectionPanel.repaint()
    }
}

fun createLabelWithDescription(title: String, description: String): JBLabel {
    val strongTitle = "<html><strong>$title</strong></html>"
    val label = JBLabel(strongTitle, AllIcons.General.ContextHelp, JBLabel.LEFT)
    label.toolTipText = description
    return label
}

fun createWarningLabel(): JBLabel {
    val warningLabel = JBLabel("")
    warningLabel.foreground = JBColor.RED
    warningLabel.isVisible = false
    return warningLabel
}
