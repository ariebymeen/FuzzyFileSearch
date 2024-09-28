package com.quickfilesearch.settings

import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
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
    var useFzfCheckbox = JBCheckBox()
    var searchOnlyFilesInVersionControlCheckbox = JBCheckBox()
    var pathDisplayDropdownBox = ComboBox(PathDisplayType.values());
    var warningText = createWarningLabel()
    var openRelativeFileActionsTable = ActionsTable(arrayOf("Name", "Reference file", "Open path", "Shortcut"), arrayOf("MyActionName", "Regex", "src/%name%Test.cc", "alt shift P"))
    var searchPathActionsTable = ActionsTable(arrayOf("Name", "Path", "Extension", "Shortcut"), arrayOf("ActionName", "/files", "txt", "alt shift H"))
    var searchRelativeFileActionsTable = ActionsTable(arrayOf("Name", "Reference file", "Extension", "Shortcut"), arrayOf("MyActionName", "Regex", "h", "alt shift P"))
    var searchRecentFiles = StaticTable(arrayOf("Name", "History length", "Extension", "Shortcut"), arrayOf(arrayOf("SearchRecentFiles", "10", "", "alt shift R")))
    var searchOpenFiles = StaticTable(arrayOf("Name", "Extension", "Shortcut"), arrayOf(arrayOf("SearchOpenFiles", "", "alt shift O")))
//    var invalidateHashesButton = JButton("Restore hashes")

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
                createLabelWithDescription("Use fzf for search", """
                    If checked the searching is done by fzf. Can only be selected if fzf is installed and available in your path. 
                    Will only work on linux. It not checked, searching is done using a custom (naive) implementation. It is recommended
                    that you install fzf and enable this option
                """.trimIndent()), useFzfCheckbox)
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
                createLabelWithDescription("Shrink the search area to only the shown results", """
                    If checked the search area will shrink to the number of results. Else the search area height
                    will always be the configured height
                """.trimIndent()), shrinkSearchAreaWithResults)

            // Create Relative file opening actions
            .addSeparator()
            .addComponent(warningText)
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
                    Specify the extension you want to search for, if empty all are included.
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
//            .addComponent(invalidateHashesButton)

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
