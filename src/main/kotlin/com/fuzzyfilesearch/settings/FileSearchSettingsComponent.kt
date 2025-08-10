package com.fuzzyfilesearch.settings

import com.fuzzyfilesearch.actions.ActionType
import com.fuzzyfilesearch.settings.actionView.ActionViewWrapper
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.invokeLater
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

class FileSearchSettingsComponent(val mSettings: GlobalSettings.SettingsState) {
    var panel: JPanel

    var keeper = SettingsComponentKeeper()

    val regexTestComponent = RegexTestComponent()
    val showHelpButton = JButton("Show help")
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
    val actionTypes = ActionType.values().take(ActionType.values().size - 2).toTypedArray()
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
            .addComponent(JBLabel("<html><strong>File search settings for FuzzyFileSearch</strong></html>"))
            .addSeparator()

        keeper.createJBIntSpinnerComponent(
            mSettings.file::numberOfFilesInSearchView, 10, 1, 100, 1, builder,
            "Max number of files visible in search view", """
            Sets the maximum number of files in the search view. Defaults to 10, as this is the most that can be selected
            by number. Setting this value too high can seriously affect the rendering performance. Not that this only affects
            the number of files visible, all other files are still used in searching""".trimIndent())
        keeper.createComboboxComponent(
            mSettings::filePathDisplayType, PathDisplayType.values(), builder, "Path display type",
            """Select how you want to display the files in the search menu""".trimIndent())
        val policyCombobox = keeper.createComboboxComponent(
            mSettings.file::popupSizePolicy, PopupSizePolicy.values(), builder, "Popup scaling", """
               Select how the popup resizes. Fixed size will allow you to specify the size in pixels. Resize with
               ide bounds: specify the size of the popup as a fraction of the ide size. Resize with screen size: 
               specify the size of the popup as a fraction of the screen size. 
               This may give unexpected behaviour on multi-monitor setups""".trimIndent())
        val searchPopupWidthSpinner = keeper.createJSpinnerComponent(
            mSettings.file::searchPopupWidth,
            0.3,
            0.1,
            1.0,
            0.05,
            builder,
            "Search view width fraction",
            """The width of the search popup as a fraction of the screen width """.trimIndent())
        val searchPopupHeightSpinner = keeper.createJSpinnerComponent(
            mSettings.file::searchPopupHeight,
            0.3,
            0.1,
            1.0,
            0.05,
            builder,
            "Search view height fraction",
            """The width of the search popup as a fraction of the screen height""".trimIndent())
        val searchPopupWidthPxSpinner = keeper.createJBIntSpinnerComponent(
            mSettings.file::searchPopupWidthPx, 700, 50, 10000, 5, builder,
            "Fixed width of popup in pixels",
            """ Use a fixed size popup. This sets the width of the popup in pixels irrespective of the screen or ide size""".trimIndent())
        val searchPopupHeightPxSpinner = keeper.createJBIntSpinnerComponent(
            mSettings.file::searchPopupHeightPx, 700, 50, 10000, 5, builder,
            "Fixed height of popup in pixels", """
                    Use a fixed size popup. This sets the height of the popup in pixels irrespective of the screen or ide size""".trimIndent())
        keeper.createJSpinnerComponent(
            mSettings.file::horizontalPositionOnScreen,
            0.5,
            0.1,
            1.0,
            0.01,
            builder,
            "X Position of search area on screen",
            """Relative X position on screen. 0 means all the way left, 1 means all the way right""".trimIndent())
        keeper.createJSpinnerComponent(
            mSettings.file::verticalPositionOnScreen,
            0.5,
            0.1,
            1.0,
            0.01,
            builder,
            "Y Position of search area on screen",
            """Relative Y position on screen. 0 means all the way left, 1 means all the way right""".trimIndent())
        keeper.createJBIntSpinnerComponent(
            mSettings.file::searchBarHeight, 30, 10, 300, 1, builder,
            "Height of the search bar in pixels", """""".trimIndent())
        keeper.createJBIntSpinnerComponent(
            mSettings.file::searchItemHeight, 30, 10, 100, 1, builder,
            "Height of the search items in pixels", """""".trimIndent())
        keeper.createCheckboxComponent(
            mSettings.file::showFileIcon, builder, "Show file icon",
            """""".trimIndent())
        keeper.createCheckboxComponent(
            mSettings.file::showNumberInSearchView, builder, "Show the index of each item in search view",
            """If checked show the number (index) of the item in the view as a number in front of the result """.trimIndent())
        val shrinkViewCheckbox = keeper.createCheckboxComponent(
            mSettings.file::shrinkViewDynamically, builder, "Shrink the search area to only the found results",
            """If checked the search area will shrink to the number of results. Else the search area height
                will always be the configured height""".trimIndent())
        val showEditorPreviewCheckbox = keeper.createCheckboxComponent(
            mSettings.file::showEditorPreview, builder, "Show editor preview",
            """ If checked, a small editor will be shown with the selected file contents. Can be used to quickly 
                    edit files. May negatively impact the performance. If selected, shrinking the search box is not supported""".trimIndent())
        val editorLocation = keeper.createComboboxComponent(
            mSettings.file::editorPreviewLocation, EditorLocation.values(), builder, "Location of the editor preview",
            """Show the preview editor either below or to the right of the search box""".trimIndent())
        val editorMinSize = keeper.createJBIntSpinnerComponent(
            mSettings.file::minSizeEditorPx, 200, 40, 5000, 5, builder,
            "Min size of the editor view in pixels", """Minimum size of the editor in pixels. 
                    If the popup size is scaled with the size of the editor and the editor size is below this value, the editor is hidden. """.trimIndent())
        val editorSize = keeper.createJSpinnerComponent(
            mSettings.file::editorSizeRatio, 0.5, 0.1, 1.0, 0.01, builder, "Editor preview ratio", """
                    The ratio of the preview editor size as a fraction of the total width or height of the popup. 
                    If the preview editor is shown below the search area, the fraction of the total height will be selected.
                    If the preview editor is shown to the right of the search area, the fraction of the total width will be selected""".trimIndent())

        val fileNameOnlyCheckbox = keeper.createCheckboxComponent(
            mSettings::searchFileNameOnly, builder, "Only match file name in search", """
                When true, only the filename is used to search items, otherwise, both the path and the filename are used for matching searching
            """.trimIndent())
        val fileNameMultiplier = keeper.createJSpinnerComponent(
            mSettings::searchFileNameMultiplier,
            1.0,
            1.0,
            10.0,
            0.1,
            builder, "Matching weight for the file name", """
                Increase or decrease the importance of the filename during searching. If 1, the path and the filename are
                matched equally, if more than one, the filename match is weighted more
            """.trimIndent())
        keeper.createComboboxComponent(
            mSettings::showSearchDirectoryPolicy, ShowSearchDirectoryPolicy.values(), builder, "Show search directory in search view",
            """Shows the search directory to the left of the search box. """.trimIndent())
        keeper.createJBIntSpinnerComponent(mSettings::showSearchDirectoryCutoffLen,20, 4, 100, 1, builder,
                                           "Search directory cutoff length", """Maximum length of the search directory string, if 
                                               longer than the max, the string is cutoff""".trimMargin())
        if (mSettings.common.enableDebugOptions) {
            keeper.createCheckboxComponent(
                mSettings.file::searchMultiThreaded,
                builder,
                "Testoption, search multithreaded",
                "")
        }

        // Create Relative file opening actions
        builder.addSeparator()
            .addComponent(JBLabel("Actions"))

        refreshActionsPanel()
        builder.addComponent(actionsCollectionPanel)
            .addComponent(rbPanel)
            .addSeparator()

        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(showHelpButton)
            add(exportButton)
            add(importButton)
        }
        builder.addComponent(buttonPanel)
            .addSeparator()
            .addComponent(JBLabel("Test your regex below"))
            .addComponent(regexTestComponent)
            .addComponentFillVertically(JPanel(), 0)


        panel = builder.panel

        showHelpButton.addActionListener {
            // Trigger ShowHelpDialog action
            val action = ActionManager.getInstance().getAction("com.fuzzyfilesearch.actions.ShowHelpDialog")
            val event = AnActionEvent(
                null,
                DataManager.getInstance().dataContext,
                "", Presentation(), ActionManager.getInstance(), 0)
            action.actionPerformed(event)
        }

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

                val searchWithFileNameOnly = fileNameOnlyCheckbox.isSelected
                fileNameMultiplier.isEnabled = !searchWithFileNameOnly
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
        fileNameOnlyCheckbox.addActionListener { e ->
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


