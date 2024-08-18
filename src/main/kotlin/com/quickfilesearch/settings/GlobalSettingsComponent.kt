package com.quickfilesearch.settings

import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel


class GlobalSettingsComponent {
    var panel: JPanel

    var excludedDirs = JBTextArea()
    var nofVisibleFilesInSearchViewSelector = JBIntSpinner(10, 1, 100)
    val searchBoxWidth = JSpinner(SpinnerNumberModel(0.3.toDouble(), 0.1.toDouble(), 1.0.toDouble(), 0.05.toDouble()))
    val searchBoxHeight = JSpinner(SpinnerNumberModel(0.3.toDouble(), 0.1.toDouble(), 1.0.toDouble(), 0.05.toDouble()))
    var useFzfCheckbox = JBCheckBox()
    var pathDisplayDropdownBox = ComboBox(PathDisplayType.values());
    var warningText = createWarningLabel()
    var openRelativeFileActionsTable = ActionsTable(arrayOf("Name", "Reference file", "Open path", "Shortcut"), arrayOf("MyActionName", "Regex", "src/%name%Test.cc", "alt shift P"))
    var searchPathActionsTable = ActionsTable(arrayOf("Name", "Path", "Extension", "Shortcut"), arrayOf("ActionName", "/files", "txt", "alt shift H"))
    var searchRelativeFileActionsTable = ActionsTable(arrayOf("Name", "Reference file", "Extension", "Shortcut"), arrayOf("MyActionName", "Regex", "h", "alt shift P"))

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
                    The height of the search popup as a fraction of the screen height 
                """.trimIndent()), searchBoxHeight)

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

            // Create Relative file opening actions
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

//fun createStrongLabel(title: String) : JBLabel {
//    val strongTitle = "<html><strong>$title</strong></html>"
//    val label = JBLabel(strongTitle, JBLabel.LEFT)
//    return label
//}
//
//fun createLabel(title: String) : JPanel {
//    val label = JBLabel(title, JBLabel.LEFT)
//    label.setAllowAutoWrapping(true)
//    val panel = JPanel(BorderLayout())
//    panel.add(label, BorderLayout.CENTER)
//    return panel
//}
//
//fun createTextArea(text: String) : JBTextArea {
//    val textArea = JBTextArea(text)
//    textArea.lineWrap = true
//    return textArea
//}
