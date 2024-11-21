package com.fuzzyfilesearch.settings

import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.util.preferredWidth
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel


class GlobalSettingsComponent {
    var panel: JPanel

    var excludedDirs = JBTextArea()
    var nofVisibleFilesInSearchViewSelector = JBIntSpinner(10, 1, 100)
    val searchBoxWidth = JSpinner(SpinnerNumberModel(0.3.toDouble(), 0.1.toDouble(), 1.0.toDouble(), 0.05.toDouble()))
    val searchBoxHeight = JSpinner(SpinnerNumberModel(0.3.toDouble(), 0.1.toDouble(), 1.0.toDouble(), 0.05.toDouble()))
    val searchBoxPosX = JSpinner(SpinnerNumberModel(0.5.toDouble(), 0.1.toDouble(), 1.0.toDouble(), 0.01.toDouble()))
    val searchBoxPosY = JSpinner(SpinnerNumberModel(0.5.toDouble(), 0.1.toDouble(), 1.0.toDouble(), 0.01.toDouble()))
    var searchBarHeight = JBIntSpinner(30, 10, 300)
    var searchItemHeight = JBIntSpinner(30, 10, 100)
    var shrinkSearchAreaWithResults = JBCheckBox()
    var searchCaseSensitiviyCheckbox = JBCheckBox()
    var showEditorPreviewCheckbox = JBCheckBox()
    var openFilesSingleClick = JBCheckBox()
    var editorPreviewLocation = ComboBox(EditorLocation.values());
    val editorSizeRatio = JSpinner(SpinnerNumberModel(0.5.toDouble(), 0.1.toDouble(), 1.0.toDouble(), 0.01.toDouble()))
    var searchOnlyFilesInVersionControlCheckbox = JBCheckBox()
    var pathDisplayDropdownBox = ComboBox(PathDisplayType.values());
    var warningText = createWarningLabel()
    var openRelativeFileActionsTable = ActionsTable(arrayOf("Name", "Reference file", "Open path", "Shortcut"), arrayOf("MyActionName", "Regex", "src/%name%Test.cc", "alt shift P"))
    var searchPathActionsTable = ActionsTable(arrayOf("Name", "Path", "Extensions", "Shortcut"), arrayOf("ActionName", "/", ".txt, .md", "alt shift H"))
    var searchRelativeFileActionsTable = ActionsTable(arrayOf("Name", "Reference file", "Extensions", "Shortcut"), arrayOf("MyActionName", "Regex", "h", "alt shift P"))
    var searchRecentFiles = StaticTable(arrayOf("Name", "History length", "Extensions", "Shortcut"), arrayOf(arrayOf("SearchRecentFiles", "10", ".txt,.md", "alt shift R")))
    var searchOpenFiles = StaticTable(arrayOf("Name", "Extensions", "Shortcut"), arrayOf(arrayOf("SearchOpenFiles", ".txt,.md", "alt shift O")))

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
            // height and width of search box
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
                createLabelWithDescription("Editor preview ratio", """
                    The ratio of the preview editor size as a fraction of the total width or height of the popup. 
                    If the preview editor is shown below the search area, the fraction of the total height will be selected.
                    If the preview editor is shown to the right of the search area, the fraction of the total width will be selected.
                """.trimIndent()), editorSizeRatio)

            // Create Relative file opening actions
            .addSeparator()
            .addComponent(warningText)
            .addComponent(createLabelWithDescription("More information", """
                The tables below can be edited to create your custom search and open actions.
                The "Name" field if the action refers to the action name in the intelij editor and must be unique.
                You can find and test this action using 'Help->Find Action' and searching for the name. The action is registered under the name
                com.fuzzyfilesearch.%ACTION_NAME%. This can be uses for ideavim integration, where these actions can be triggered by referencing this name.
                The "Reference File" field expects a valid regex that is uses to search for a file with a specific name. Note that it only matches the filename, not the path.
                E.g. if you enter [A-Za-z]+\.txt it will match any .txt file. The directory will be found that contains a file that matches this pattern in 
                the directories above the current file, the directory tree will not be traversed down to search for the file.
                The "Extensions" field is an optional field, if filled, only the files with these extensions are used in the search.
                To enter multiple extension, separate these with a ',' or a '|'. The '.' in front of the extension is optional. Valid examples: ".txt,.md" and "txt | md"
                The "Shortcut" field is an optional field that assigns a shortcut to the action. If this field is empty, no shortcut is set. 
                If the shortcut does not seem to work, validate that the action is registered (search in "Help->Find Action"). If the action is registered correctly, the shortcut might
                already be in use for something else. Remove the shortcut for the other action in the settings, and try again. 
                """.trimIndent()))
            .addComponent(
                createLabelWithDescription("Create action for opening relative file", """
                Open a file that is related to the currently open file. If no regex is entered, %name% is set to the name of the current file (without extension).
                If not empty, %name% is set to the name of the file that matches the regex that is closest to the currently open file (without extension).
                The action to open the file starts from the reference file directory, so enter a relative path.
                 """.trimIndent())
            )
            .addComponent(openRelativeFileActionsTable)

            // Create relative search actions table
            .addSeparator()
            .addComponent(
                createLabelWithDescription("Create action for searching files related to relative file", """
                Search in all files next to or below the file satisfying regex closest to the open file. If no regex is entered, the location of the
                open file is used. For example: Use CmakeList.txt as reference, search action will search all files in the same and lower directories of the 
                folder containing this file. Note that 'closest' means closest up the file tree, it does not look down. If no file satisfying the regex is found, 
                the search directory is set to the directory at the maximum search distance. If no regex is entered, %name% is set to the name of the current file (without extension).
                """.trimIndent())
            )
            .addComponent(searchRelativeFileActionsTable)

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

            .addComponentFillVertically(JPanel(), 0)
            .panel
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
