package com.openrelativefile.Settings

import com.intellij.icons.AllIcons
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel


class ProjectSettingsComponent {
    var panel: JPanel

    var warningText = createWarningLabel()
    var openRelativeFileActionsTable = ActionsTable(arrayOf("Name", "Reference file", "Open path", "Shortcut"), arrayOf("MyActionName", "Regex", "src/%name%Test.cc", "alt shift P"))
    var searchPathActionsTable = ActionsTable(arrayOf("Name", "Path", "Extension", "Shortcut"), arrayOf("ActionName", "/files", "txt", "alt shift H"))
    var searchRelativeFileActionsTable = ActionsTable(arrayOf("Name", "Reference file", "Extension", "Shortcut"), arrayOf("MyActionName", "Regex", "h", "alt shift P"))

    init {
        panel = FormBuilder()
            .addComponent(JBLabel("<html><strong>Project specific settings for OpenRelativeFile</strong></html>"))
            .addSeparator()
            .addComponent(warningText)

            // Create Relative file opening actions
            .addSeparator()
            .addComponent(
                createLabelWithDescription("Create action for opening relative file", """
                Open a file that is related to the currently open file. If no regex is entered, %name% is set to the name of the current file (without extension).
                If not empty, %name% is set to the name of the file that matches the regex that is closest to the currently open file (without extension).
                The action to open the file starts from the reference file directory, so enter a relative path.
                These settings are in addition to the global settings and will not override these.
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
                These settings are in addition to the global settings and will not override these.
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
