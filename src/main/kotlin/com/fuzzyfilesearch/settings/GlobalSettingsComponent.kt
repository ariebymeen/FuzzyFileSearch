package com.fuzzyfilesearch.settings

import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.*
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.*


class GlobalSettingsComponent {
    var panel: JPanel

    var excludedDirs = JBTextArea()
    var nofVisibleFilesInSearchViewSelector = JBIntSpinner(10, 1, 100)
    var popupSizePolicySelector = ComboBox(PopupSizePolicy.values());
    val searchBoxWidth = JSpinner(SpinnerNumberModel(0.3, 0.1, 1.0, 0.05))
    val searchBoxHeight = JSpinner(SpinnerNumberModel(0.3, 0.1, 1.0, 0.05))
    val searchBoxPosX = JSpinner(SpinnerNumberModel(0.5, 0.1, 1.0, 0.01))
    val searchBoxPosY = JSpinner(SpinnerNumberModel(0.5, 0.1, 1.0, 0.01))
    var searchBoxWidthPx = JBIntSpinner(700, 50, 10000, 5)
    var searchBoxHeightPx = JBIntSpinner(500, 50, 10000, 5)
    var minSizeEditorPx = JBIntSpinner(200, 50, 5000, 5)
    var searchBarHeight = JBIntSpinner(30, 10, 300)
    var searchItemHeight = JBIntSpinner(30, 10, 100)
    var useDefaultFontCheckbox = JBCheckBox()
    var fontSelectorDropdown = FontComboBox()
    var useDefaultHighlightColorCheckbox = JBCheckBox()
    var colorSelectorElement = ColorPanel()
    var fontSize = JBIntSpinner(13, 1, 30)
    var shrinkSearchAreaWithResults = JBCheckBox()
    var searchCaseSensitiviyCheckbox = JBCheckBox()
    var showEditorPreviewCheckbox = JBCheckBox()
    var openFilesSingleClick = JBCheckBox()
    var editorPreviewLocation = ComboBox(EditorLocation.values());
    val editorSizeRatio = JSpinner(SpinnerNumberModel(0.5, 0.1, 1.0, 0.01))
    var searchOnlyFilesInVersionControlCheckbox = JBCheckBox()
    var pathDisplayDropdownBox = ComboBox(PathDisplayType.values())
    var modifierKeyDropdownBox = ComboBox(ModifierKey.values())
    var openFileInVerticalSplitShortcutInputBox = JBTextField()
    var openFileInHorizontalSplitShortcutInputBox = JBTextField()
    var openFileInActiveEditorShortcutInputBox = JBTextField()
    var warningText = createWarningLabel()
    var openRelativeFileActionsTable = ActionsTable(arrayOf("Name", "Reference file", "Open path", "Shortcut"), arrayOf("MyActionName", "Regex", "src/%rname%Test.cc", "alt shift P"))
    var searchPathActionsTable = ActionsTable(arrayOf("Name", "Path", "Extensions", "Shortcut"), arrayOf("ActionName", "/", ".txt, .md", "alt shift H"))
    var searchRelativeFileActionsTable = ActionsTable(arrayOf("Name", "Reference file", "Extensions", "Shortcut"), arrayOf("MyActionName", "Regex", "h", "alt shift P"))
    var searchFileMatchingPatternActionsTable = ActionsTable(arrayOf("Name", "Path", "Pattern (Regex)", "Shortcut"), arrayOf("MyActionName", "/", "Regex", "alt shift P"))
    var searchRecentFiles = StaticTable(arrayOf("Name", "History length", "Extensions", "Shortcut"), arrayOf(arrayOf("SearchRecentFiles", "10", ".txt,.md", "alt shift R")))
    var searchOpenFiles = StaticTable(arrayOf("Name", "Extensions", "Shortcut"), arrayOf(arrayOf("SearchOpenFiles", ".txt,.md", "alt shift O")))
    // TODO: Add options for a shortcut to open file in horizontal or vertical split
    val regexTestComponent = RegexTestComponent()
    val showHelpButton = JButton("Show help")

    init {

        panel = FormBuilder()
            .addComponent(JBLabel("<html><strong>Settings for QuickFileSearch</strong></html>"))
            .addSeparator()

            .addComponent(createLabelWithDescription("Excluded folders", "Wildcards are not supported, enter the full name of the folder"))
            .addComponent(excludedDirs)
            .addLabeledComponent(
                createLabelWithDescription("Max number of files visible in search view", """
                    Sets the maximum number of files in the search view. Defaults to 10, as this is the most that can be selected
                    by number. Setting this value too high can seriously affect the rendering performance. Not that this only affects
                    the number of files visible, all other files are still used in searching
                """.trimIndent()), nofVisibleFilesInSearchViewSelector)
            .addLabeledComponent(
                createLabelWithDescription("Search case sensitive", """
                    If checked the searching algorithm is case sensitive.
                """.trimIndent()), searchCaseSensitiviyCheckbox)
            .addLabeledComponent(
                createLabelWithDescription("Search only files that are tracked by vcs", """
                    If checked only files that are tracked by a version control system (vcs) are searched.
                    Else all files are part of the search (except for directories explicitly excluded)
                """.trimIndent()), searchOnlyFilesInVersionControlCheckbox)
            .addLabeledComponent(
                createLabelWithDescription("Path display type", """
                    Select how you want to display the files in the search menu.
                """.trimIndent()), pathDisplayDropdownBox)
            .addLabeledComponent(
                createLabelWithDescription("Modifier key", """
                    The results can be scrolled through using the arrow up/down keys or using the j/k key if the modifier
                    key is pressed. This key can be either the ctrl or the alt key.
                    The modifier key can also be used in combination with the number keys to quickly open a result, e.g. ctrl-1 to open result 1 (second result). 
                    This can be used as an alternative to scrolling to the result and pressing enter.
                """.trimIndent()), modifierKeyDropdownBox)
            .addLabeledComponent(
                createLabelWithDescription("Shortcut open file in vertical split", """
                    Set the shortcut for opening the selected file in a vertical split view
                """.trimIndent()), openFileInVerticalSplitShortcutInputBox)
            .addLabeledComponent(
                createLabelWithDescription("Shortcut open file in horizontal split", """
                    Set the shortcut for opening the selected file in a horizontal split view
                """.trimIndent()), openFileInHorizontalSplitShortcutInputBox)
            .addLabeledComponent(
                createLabelWithDescription("Shortcut open file in aive editor", """
                    Apart from using enter, a custom shortcut can be added to open the currently selected file in the active edito
                """.trimIndent()), openFileInActiveEditorShortcutInputBox)
            // height and width of search box
            .addLabeledComponent(
                createLabelWithDescription("Popup scaling", """
                    Select how the popup resizes. Fixed size will allow you to specify the size in pixels. Resize with
                    ide bounds: specify the size of the popup as a fraction of the ide size. Resize with screen size: 
                    specify the size of the popup as a fraction of the screen size. 
                    This may give unexpected behaviour on multi-monitor setups.
                """.trimIndent()), popupSizePolicySelector)
            .addLabeledComponent(
                createLabelWithDescription("Search view width fraction", """
                    The width of the search popup as a fraction of the screen width
                """.trimIndent()), searchBoxWidth)
            .addLabeledComponent(
                createLabelWithDescription("Search view height fraction", """
                    The height of the search popup as a fraction of the screen height. If shrinking is enabled, this is
                    the maximum height of the view
                """.trimIndent()), searchBoxHeight)
            .addLabeledComponent(
                createLabelWithDescription("Fixed width of popup in pixels", """
                    Use a fixed size popup. This sets the width of the popup in pixels irrespective of the screen or ide size
                """.trimIndent()), searchBoxWidthPx)
            .addLabeledComponent(
                createLabelWithDescription("Fixed height of popup in pixels", """
                    Use a fixed size popup. This sets the height of the popup in pixels irrespective of the screen or ide size
                """.trimIndent()), searchBoxHeightPx)
            .addLabeledComponent(
                createLabelWithDescription("X Position of search area on screen", """
                    Relative X position on screen. 0 means all the way left, 1 means all the way right
                """.trimIndent()), searchBoxPosX)
            .addLabeledComponent(
                createLabelWithDescription("Y Position of search area on screen", """
                    Relative Y position on screen. 0 means all the way at the top, 1 means all the way down
                """.trimIndent()), searchBoxPosY)
            .addLabeledComponent(
                createLabelWithDescription("Height of the search bar in pixels", """
                """.trimIndent()), searchBarHeight)
            .addLabeledComponent(
                createLabelWithDescription("Height of the search items in pixels", """
                """.trimIndent()), searchItemHeight)
            .addLabeledComponent(
                createLabelWithDescription("Use default popup font", """
                    If checked use the same font as the editor, else a font can be selected
                """.trimIndent()), useDefaultFontCheckbox)
            .addLabeledComponent(
                createLabelWithDescription("Select popup font", """
                    Choose the font to be used in poupp
                """.trimIndent()), fontSelectorDropdown)
            .addLabeledComponent(
                createLabelWithDescription("Popup font size", """
                    Choose the font size
                """.trimIndent()), fontSize)
            .addLabeledComponent(
                createLabelWithDescription("Use default highlight color", """
                """.trimIndent()), useDefaultHighlightColorCheckbox)
            .addLabeledComponent(
                createLabelWithDescription("Choose the highlight color", """
                    Choose the highlight color to be used in popup
                """.trimIndent()), colorSelectorElement)
            .addLabeledComponent(
                createLabelWithDescription("Shrink the search area to only the found results", """
                    If checked the search area will shrink to the number of results. Else the search area height
                    will always be the configured height
                """.trimIndent()), shrinkSearchAreaWithResults)
            .addLabeledComponent(
                createLabelWithDescription("Open file with a single click", """
                    If checked, open the file in a single click. Opening the file in the preview is then only possible 
                    using the keyboard. If not clicked, the item must be double clicked to open the file.
                """.trimIndent()), openFilesSingleClick)

            // Editor preview settings
            .addLabeledComponent(
                createLabelWithDescription("Show editor preview", """
                    If checked, a small editor will be shown with the selected file contents. Can be used to quickly 
                    edit files. May negatively impact the performance. If selected, shrinking the search box is not supported.
                """.trimIndent()), showEditorPreviewCheckbox)
            .addLabeledComponent(
                createLabelWithDescription("Location of the editor preview", """
                    Show the preview editor either below or to the right of the search box
                """.trimIndent()), editorPreviewLocation)
            .addLabeledComponent(
                createLabelWithDescription("Min size of the editor view in pixels", """
                    Minimum size of the editor in pixels. If the popup size is scaled with the size of the editor and the editor
                    size is below this value, the editor is hidden.
                """.trimIndent()), minSizeEditorPx)
            .addLabeledComponent(
                createLabelWithDescription("Editor preview ratio", """
                    The ratio of the preview editor size as a fraction of the total width or height of the popup. 
                    If the preview editor is shown below the search area, the fraction of the total height will be selected.
                    If the preview editor is shown to the right of the search area, the fraction of the total width will be selected.
                """.trimIndent()), editorSizeRatio)

            // Create Relative file opening actions
            .addSeparator()
            .addComponent(warningText)
            .addComponent(
                createLabelWithDescription("Create action for opening relative file", """
                Open a file that is related to the currently open file. If no regex is entered, %name% is set to the name of the current file (without extension).
                If not empty, %rname% is set to the name of the file that matches the regex that is closest to the currently open file (without extension).
                The action to open the file starts from the reference file directory, so enter a relative path. 
                The %cname% variable is set to the name of the currently open file. This name is compared with the files in the open path.
                If a file in the directory matches partly, it is considered to be the same 
                (if the current filename is MyFileTest it will open the file MyFile unless MyFileTest also exists in the open path).
                Note that %rname% and %cname% cannot be used at the same time. If you want to have multiple options use the | to split them. The options
                are evaluated in order.
                 """.trimIndent())
            )
            .addComponent(openRelativeFileActionsTable)

            // Create relative search actions table
            .addSeparator()
            .addComponent(
                createLabelWithDescription("Create action for searching files related to relative path", """
                Search in all files next to or below the file satisfying regex closest to the open file. If no regex is entered, the location of the
                open file is used. For example: Use CmakeList.txt as reference, search action will search all files in the same and lower directories of the 
                folder containing this file. Note that 'closest' means closest up the file tree, it does not look down. If no file satisfying the regex is found, 
                the search directory is set to the directory at the maximum search distance. If no regex is entered, %name% is set to the name of the current file (without extension).
                """.trimIndent())
            )
            .addComponent(searchRelativeFileActionsTable)

            // Create search files matching pattern actions table
            .addSeparator()
            .addComponent(
                createLabelWithDescription("Search for files matching pattern", """
                    Search through all files where the filename matches a regex
                """.trimIndent())
            )
            .addComponent(searchFileMatchingPatternActionsTable)

            // Create file in path search actions
            .addSeparator()
            .addComponent(
                createLabelWithDescription("Search in path", """
                    Search in a path. Use / as first character searches in the folder that is open in the editor. 
                    Start with . to create a relative path (./ searches in directory of currently open file ../ in its parent etc.)
                    Specify the extensions you want to search for, if empty all are included.
                """.trimIndent())
            )
            .addComponent(searchPathActionsTable)

            .addComponent(
                createLabelWithDescription("Search in most recently opened files", """
                    Search through all the files that you have opened most recently. Also includes all files that are currently open in your editor.
                    If the extension is empty, include all.
                """.trimIndent())
            )
            .addComponent(searchRecentFiles)

            .addComponent(
                createLabelWithDescription("Search in all files currently open in editor", """
                    Search through files currently open in your editor with extension. If extension is empty, include all.
                """.trimIndent())
            )
            .addComponent(searchOpenFiles)

            .addSeparator()
            .addComponent(JBLabel("Test your regex below"))
            .addComponent(regexTestComponent)
            .addComponent(showHelpButton)

            .addComponentFillVertically(JPanel(), 0)
            .panel

        showHelpButton.addActionListener {
            // Trigger ShowHelpDialog action
            val action = ActionManager.getInstance().getAction("com.fuzzyfilesearch.actions.ShowHelpDialog")
            val event = AnActionEvent(null,
                DataManager.getInstance().dataContext,
                "", Presentation(), ActionManager.getInstance(), 0)
            action.actionPerformed(event)
        }
    }

    fun setEditorScalingFields() {
        val staticSizeEnabled = (popupSizePolicySelector.selectedItem as PopupSizePolicy) == PopupSizePolicy.FIXED_SIZE
        searchBoxWidthPx.isEnabled = staticSizeEnabled
        searchBoxHeightPx.isEnabled = staticSizeEnabled
        searchBoxWidth.isEnabled = !staticSizeEnabled
        searchBoxHeight.isEnabled = !staticSizeEnabled
    }
}

fun createLabelWithDescription(title: String, description: String): JBLabel {
    val strongTitle = "<html><strong>$title</strong></html>"
    val label = JBLabel(strongTitle, AllIcons.General.ContextHelp, JBLabel.LEFT)
    label.toolTipText = description
    return label
}

fun createWarningLabel() : JBLabel {
    val warningLabel = JBLabel("")
    warningLabel.foreground = JBColor.RED
    warningLabel.isVisible = false
    return warningLabel
}
