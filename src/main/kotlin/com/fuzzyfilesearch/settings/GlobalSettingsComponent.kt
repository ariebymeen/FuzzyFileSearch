package com.fuzzyfilesearch.settings

import com.fuzzyfilesearch.actions.*
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.ui.ColorPanel
import com.intellij.ui.FontComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import javax.swing.JButton
import javax.swing.JPanel
import kotlin.reflect.KMutableProperty0

// TODO: Create settings components that make this all a bit less manual
// TODO: Make popup size independent between string search and file search

class GlobalSettingsComponent(val mSettings: GlobalSettings.SettingsState) {
    var panel: JPanel

    val components = mutableListOf<SettingsComponent>()

    var excludedDirs = JBTextArea()
    var fontSelectorDropdown = FontComboBox()
    var colorSelectorElement = ColorPanel()
    var warningText = createWarningLabel()
    val regexTestComponent = RegexTestComponent()
    val showHelpButton = JButton("Show help")

    init {

        val builder = FormBuilder()
            .addComponent(JBLabel("<html><strong>Settings for FuzzyFileSearch</strong></html>"))
            .addSeparator()

            .addComponent(createLabelWithDescription("Excluded folders", "Wildcards are not supported, enter the full name of the folder"))
            .addComponent(excludedDirs)

        createJBIntSpinnerComponent(mSettings::numberOfFilesInSearchView, 10, 1, 100, 1, builder,
            "Max number of files visible in search view", """
            Sets the maximum number of files in the search view. Defaults to 10, as this is the most that can be selected
            by number. Setting this value too high can seriously affect the rendering performance. Not that this only affects
            the number of files visible, all other files are still used in searching""".trimIndent())
        createCheckboxComponent(mSettings::searchCaseSensitivity, builder, "Search case sensitive",
            "If checked the searching algorithm is case sensitive")
        createCheckboxComponent(mSettings::searchOnlyFilesInVersionControl, builder, "Search only files that are tracked by vcs",
            """ If checked only files that are tracked by a version control system (vcs) are searched.
                Else all files are part of the search (except for directories explicitly excluded)""".trimIndent())
        createComboboxComponent(mSettings::filePathDisplayType, PathDisplayType.values(), builder, "Path display type",
            """Select how you want to display the files in the search menu""".trimIndent())
        createComboboxComponent(mSettings::modifierKey, ModifierKey.values(),builder, "Modifier key", """
                    The results can be scrolled through using the arrow up/down keys or using the j/k key if the modifier
                    key is pressed. This key can be either the ctrl or the alt key.
                    The modifier key can also be used in combination with the number keys to quickly open a result, e.g. ctrl-1 to open result 1 (second result). 
                    This can be used as an alternative to scrolling to the result and pressing enter""".trimIndent())
        createTextFieldComponent(mSettings::openInVerticalSplit, builder, "Shortcut open file in vertical split", """
                    Set the shortcut for opening the selected file in a vertical split view""".trimIndent())
        createTextFieldComponent(mSettings::openInHorizontalSplit, builder, "Shortcut open file in horizontal split", """
                    Set the shortcut for opening the selected file in a horizontal split view""".trimIndent())
        createTextFieldComponent(mSettings::openInActiveEditor, builder, "Shortcut open file in aive editor", """
                    Apart from using enter, a custom shortcut can be added to open the currently selected file in the active editor""".trimIndent())
        createComboboxComponent(mSettings::popupSizePolicy, PopupSizePolicy.values(), builder, "Popup scaling", """
               Select how the popup resizes. Fixed size will allow you to specify the size in pixels. Resize with
               ide bounds: specify the size of the popup as a fraction of the ide size. Resize with screen size: 
               specify the size of the popup as a fraction of the screen size. 
               This may give unexpected behaviour on multi-monitor setups""".trimIndent())
        createJSpinnerComponent(mSettings::searchPopupWidth, 0.3, 0.1, 1.0, 0.05, builder,
                "Search view width fraction", """The width of the search popup as a fraction of the screen width """.trimIndent())
        createJSpinnerComponent(mSettings::searchPopupHeight, 0.3, 0.1, 1.0, 0.05, builder,
            "Search view height fraction", """The width of the search popup as a fraction of the screen height""".trimIndent())
        createJBIntSpinnerComponent(mSettings::searchPopupWidthPx, 700, 50, 10000, 5, builder,
                "Fixed width of popup in pixels",
                """ Use a fixed size popup. This sets the width of the popup in pixels irrespective of the screen or ide size""".trimIndent())
        createJBIntSpinnerComponent(mSettings::searchPopupHeightPx, 700, 50, 10000, 5, builder,
                "Fixed height of popup in pixels", """
                    Use a fixed size popup. This sets the height of the popup in pixels irrespective of the screen or ide size""".trimIndent())
        createJSpinnerComponent(mSettings::horizontalPositionOnScreen, 0.5, 0.1, 1.0, 0.01, builder,
            "X Position of search area on screen", """Relative X position on screen. 0 means all the way left, 1 means all the way right""".trimIndent())
        createJSpinnerComponent(mSettings::verticalPositionOnScreen, 0.5, 0.1, 1.0, 0.01, builder,
            "Y Position of search area on screen", """Relative Y position on screen. 0 means all the way left, 1 means all the way right""".trimIndent())
        createJBIntSpinnerComponent(mSettings::searchBarHeight, 30, 10, 300, 1, builder,
                "Height of the search bar in pixels", """""".trimIndent())
        createJBIntSpinnerComponent(mSettings::searchItemHeight, 30, 10, 100, 1, builder,
            "Height of the search items in pixels", """""".trimIndent())
        createCheckboxComponent(mSettings::showNumberInSearchView, builder, "Show the index of each item in search view",
                """If checked show the number (index) of the item in the view as a number in front of the result """.trimIndent())
        createCheckboxComponent(mSettings::useDefaultFont, builder, "Use default popup font",
                """If checked use the same font as the editor, else a font can be selected""".trimIndent())
        builder.addLabeledComponent(
                createLabelWithDescription("Select popup font", """
                    Choose the font to be used in poupp
                """.trimIndent()), fontSelectorDropdown)
        createJBIntSpinnerComponent(mSettings::fontSize, 13, 1, 30, 1, builder, "Popup font size", """Choose the font size""")

        createCheckboxComponent(mSettings::useDefaultHighlightColor, builder, "Use default highlight color", "")
            builder.addLabeledComponent(
                createLabelWithDescription("Choose the highlight color", """
                    Choose the highlight color to be used in popup
                """.trimIndent()), colorSelectorElement)
        createCheckboxComponent(mSettings::shrinkViewDynamically, builder, "Shrink the search area to only the found results",
                """If checked the search area will shrink to the number of results. Else the search area height
                will always be the configured height""".trimIndent())
        createCheckboxComponent(mSettings::openWithSingleClick, builder, "Open file with a single click", """
                    If checked, open the file in a single click. Opening the file in the preview is then only possible 
                    using the keyboard. If not clicked, the item must be double clicked to open the file""".trimIndent())
        createCheckboxComponent(mSettings::showEditorPreview, builder, "Show editor preview",
                """ If checked, a small editor will be shown with the selected file contents. Can be used to quickly 
                    edit files. May negatively impact the performance. If selected, shrinking the search box is not supported""".trimIndent())
        createComboboxComponent(mSettings::editorPreviewLocation, EditorLocation.values(), builder, "Location of the editor preview",
            """Show the preview editor either below or to the right of the search box""".trimIndent())
        createJBIntSpinnerComponent(mSettings::minSizeEditorPx, 200, 40, 5000, 5, builder,
                "Min size of the editor view in pixels", """Minimum size of the editor in pixels. 
                    If the popup size is scaled with the size of the editor and the editor size is below this value, the editor is hidden. """.trimIndent())
        createJSpinnerComponent(mSettings::editorSizeRatio, 0.5, 0.1, 1.0, 0.01, builder, "Editor preview ratio", """
                    The ratio of the preview editor size as a fraction of the total width or height of the popup. 
                    If the preview editor is shown below the search area, the fraction of the total height will be selected.
                    If the preview editor is shown to the right of the search area, the fraction of the total width will be selected""".trimIndent())

            // Create Relative file opening actions
        builder.addSeparator()
            .addComponent(warningText)

        createActionsTableComponent(mSettings::openRelatedFileAction, builder, "Create action for opening file", """
                Open a file that is related to the currently open file. If no regex is entered, %name% is set to the name of the current file (without extension).
                If not empty, %rname% is set to the name of the file that matches the regex that is closest to the currently open file (without extension).
                The action to open the file starts from the reference file directory, so enter a relative path. 
                The %cname% variable is set to the name of the currently open file. This name is compared with the files in the open path.
                If a file in the directory matches partly, it is considered to be the same 
                (if the current filename is MyFileTest it will open the file MyFile unless MyFileTest also exists in the open path).
                Note that %rname% and %cname% cannot be used at the same time. If you want to have multiple options use the | to split them. The options
                are evaluated in order""".trimIndent(), arrayOf("Name", "Reference file", "Open path", "Shortcut"),
            arrayOf("MyActionName", "Regex", "src/%rname%Test.cc", "alt shift P"), mSettings, 0, 3, ::registerOpenRelativeFileActions)

        // Create relative search actions table
        builder.addSeparator()
        createActionsTableComponent(mSettings::searchRelativeFileActions, builder, "Create action for searching files related to relative path", """
                Search in all files next to or below the file satisfying regex closest to the open file. If no regex is entered, the location of the
                open file is used. For example: Use CmakeList.txt as reference, search action will search all files in the same and lower directories of the 
                folder containing this file. Note that 'closest' means closest up the file tree, it does not look down. If no file satisfying the regex is found, 
                the search directory is set to the directory at the maximum search distance. If no regex is entered, %name% is set to the name of the current file (without extension).
                """.trimIndent(), arrayOf("Name", "Path", "Extensions", "Shortcut"), arrayOf("ActionName", "/", ".txt, .md", "alt shift H"),
            mSettings, 0, 3, ::registerSearchRelativeFileActions)

        // Create search files matching pattern actions table
        builder.addSeparator()
        createActionsTableComponent(mSettings::searchFilesMatchingPatterActions, builder, "Search for files matching pattern", """
                    Search through all files where the filename matches a regex""".trimIndent(), arrayOf("Name", "Reference file", "Extensions", "Shortcut"),
                    arrayOf("MyActionName", "Regex", "h", "alt shift P"), mSettings, 0, 3, ::registerSearchFileMatchingPatternActions)
        // Create file in path search actions
        builder.addSeparator()
        createActionsTableComponent(mSettings::searchPathActions, builder, "Search in path", """
               Search in a path. Use / as first character searches in the folder that is open in the editor. 
               Start with . to create a relative path (./ searches in directory of currently open file ../ in its parent etc.)
               Specify the extensions you want to search for, if empty all are included.
                """.trimIndent(), arrayOf("Name", "Path", "Pattern (Regex)", "Shortcut"), arrayOf("MyActionName", "/", "Regex", "alt ctrl P"),
                mSettings, 0, 3, ::registerSearchFileInPathActions)

        createActionsTableComponent(mSettings::searchRecentFilesActions, builder, "Search in most recently opened files", """
                Search through all the files that you have opened most recently. Also includes all files that are currently open in your editor.
                If the extension is empty, include all""".trimIndent(), arrayOf("Name", "History length", "Extensions", "Shortcut"),
                arrayOf("SearchRecentFiles", "10", ".txt,.md", "alt shift R"), mSettings, 0, 3, ::registerSearchRecentFiles)
        createActionsTableComponent(mSettings::searchOpenFilesActions, builder, "Search in all files currently open in editor", """
                Search through files currently open in your editor with extension. If extension is empty, include all """.trimIndent(),
                arrayOf("Name", "Extensions", "Shortcut"), arrayOf("SearchOpenFiles", ".txt,.md", ""),
                mSettings, 0, 2, ::registerSearchOpenFiles)
        createActionsTableComponent(mSettings::searchAllFilesActions, builder, "Search in all files, including files that are not tracked by version control",
            """Special action to search through all files""".trimIndent(),arrayOf("Name", "Extensions", "Shortcut"), arrayOf("SearchAllFiles", "", "alt shift O"),
            mSettings, 0, 2, ::registerSearchAllFiles)
        createCheckboxComponent(mSettings::applySyntaxHighlightingOnTextSearch, builder, "Apply syntax highlighting on text search", """
                    If checked, apply syntax highlighting on text search results. If false, plain text is used""".trimIndent())
        createCheckboxComponent(mSettings::showEditorPreviewStringSearch, builder, "Show editor preview for pattern search", "")
        createActionsTableComponent(mSettings::searchStringMatchingPatternActions, builder, "Search for pattern in files", """
                    Search through all instances that match the pattern. If path starts with '/', search through all files. This may
                    be performance intensive if there are many files. If path is empty or '.' search only the current path. Else a relative path
                    is selected""".trimIndent(), arrayOf("Name", "Path", "Regex", "Shortcut", "Extension"),
                    arrayOf("MySearchAction", "", "Enter regex", "", ""), mSettings, 0, 3, ::registerGrepInFilesActions)

        builder.addSeparator()
            .addComponent(JBLabel("Test your regex below"))
            .addComponent(regexTestComponent)
            .addComponent(showHelpButton)

            .addComponentFillVertically(JPanel(), 0)


        panel = builder.panel

        showHelpButton.addActionListener {
            // Trigger ShowHelpDialog action
            val action = ActionManager.getInstance().getAction("com.fuzzyfilesearch.actions.ShowHelpDialog")
            val event = AnActionEvent(null,
                DataManager.getInstance().dataContext,
                "", Presentation(), ActionManager.getInstance(), 0)
            action.actionPerformed(event)
        }
    }

    // TODO: Restore functionality to enable / disable fields
    fun setEditorScalingFields() {
//        val staticSizeEnabled = (popupSizePolicySelector.selectedItem as PopupSizePolicy) == PopupSizePolicy.FIXED_SIZE
//        searchBoxWidthPx.isEnabled = staticSizeEnabled
//        searchBoxHeightPx.isEnabled = staticSizeEnabled
//        searchBoxWidth.isEnabled = !staticSizeEnabled
//        searchBoxHeight.isEnabled = !staticSizeEnabled

        // TODO: Keep track of default font (Separate from setEditorScalingFields)
//        component.fontSelectorDropdown.isEnabled = !component.useDefaultFontCheckbox.isSelected
//        component.colorSelectorElement.isEnabled = !component.useDefaultHighlightColorCheckbox.isSelected
    }

    fun createCheckboxComponent(setting: KMutableProperty0<Boolean>, builder: FormBuilder, title: String, description: String) {
        val checkbox = JBCheckboxComponent(setting)
        builder.addLabeledComponent(createLabelWithDescription(title, description.trimIndent()), checkbox.checkbox)
        components.add(checkbox)
    }

    fun createJBIntSpinnerComponent(setting: KMutableProperty0<Int>, value: Int, minValue: Int, maxValue: Int, stepSize: Int,
                                    builder: FormBuilder, title: String, description: String) {
        val spinner = JBIntSpinnerComponent(setting, value, minValue, maxValue, stepSize)
        builder.addLabeledComponent(createLabelWithDescription(title, description.trimIndent()), spinner.spinner)
        components.add(spinner)
    }

    fun createJSpinnerComponent(setting: KMutableProperty0<Double>, value: Double, minValue: Double, maxValue: Double, stepSize: Double,
                                    builder: FormBuilder, title: String, description: String) {
        val spinner = JSpinnerComponent(setting, value, minValue, maxValue, stepSize)
        builder.addLabeledComponent(createLabelWithDescription(title, description.trimIndent()), spinner.spinner)
        components.add(spinner)
    }

    fun <E> createComboboxComponent(setting: KMutableProperty0<E>, values: Array<E>,
                                    builder: FormBuilder, title: String, description: String) {
        val combobox = ComboBoxComponent<E>(setting, values)
        builder.addLabeledComponent(createLabelWithDescription(title, description.trimIndent()), combobox.combobox)
        components.add(combobox)
    }

    fun createTextFieldComponent(setting: KMutableProperty0<String>, builder: FormBuilder, title: String, description: String) {
        val textfield = JBTextFieldComponent(setting)
        builder.addLabeledComponent(createLabelWithDescription(title, description.trimIndent()), textfield.textfield)
        components.add(textfield)
    }

    fun createActionsTableComponent(setting: KMutableProperty0<Array<Array<String>>>, builder: FormBuilder, title: String, description: String,
                                    header: Array<String>, default: Array<String>, settings: GlobalSettings.SettingsState, nameIndex: Int, shortcutIndex: Int,
                                    createAction: (Array<Array<String>>, GlobalSettings.SettingsState) -> Unit) {
        val table = ActionsTableComponent(setting, header, default, nameIndex, shortcutIndex, createAction, settings)
        builder.addComponent(createLabelWithDescription(title, description))
        builder.addComponent(table.table)
        components.add(table)
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
