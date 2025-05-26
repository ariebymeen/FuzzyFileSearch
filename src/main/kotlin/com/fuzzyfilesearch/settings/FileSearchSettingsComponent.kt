package com.fuzzyfilesearch.settings

import com.fuzzyfilesearch.actions.*
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import javax.swing.JButton
import javax.swing.JPanel

class FileSearchSettingsComponent(val mSettings: GlobalSettings.SettingsState) {
    var panel: JPanel

    var keeper = SettingsComponentKeeper()

    var warningText = createWarningLabel()
    val regexTestComponent = RegexTestComponent()
    val showHelpButton = JButton("Show help")

    init {

        val builder = FormBuilder()
            .addComponent(JBLabel("<html><strong>File search settings for FuzzyFileSearch</strong></html>"))
            .addSeparator()

        keeper.createJBIntSpinnerComponent(mSettings.file::numberOfFilesInSearchView, 10, 1, 100, 1, builder,
            "Max number of files visible in search view", """
            Sets the maximum number of files in the search view. Defaults to 10, as this is the most that can be selected
            by number. Setting this value too high can seriously affect the rendering performance. Not that this only affects
            the number of files visible, all other files are still used in searching""".trimIndent())
        keeper.createComboboxComponent(mSettings::filePathDisplayType, PathDisplayType.values(), builder, "Path display type",
            """Select how you want to display the files in the search menu""".trimIndent())
        keeper.createComboboxComponent(mSettings.file::popupSizePolicy, PopupSizePolicy.values(), builder, "Popup scaling", """
               Select how the popup resizes. Fixed size will allow you to specify the size in pixels. Resize with
               ide bounds: specify the size of the popup as a fraction of the ide size. Resize with screen size: 
               specify the size of the popup as a fraction of the screen size. 
               This may give unexpected behaviour on multi-monitor setups""".trimIndent())
        keeper.createJSpinnerComponent(mSettings.file::searchPopupWidth, 0.3, 0.1, 1.0, 0.05, builder,
                "Search view width fraction", """The width of the search popup as a fraction of the screen width """.trimIndent())
        keeper.createJSpinnerComponent(mSettings.file::searchPopupHeight, 0.3, 0.1, 1.0, 0.05, builder,
            "Search view height fraction", """The width of the search popup as a fraction of the screen height""".trimIndent())
        keeper.createJBIntSpinnerComponent(mSettings.file::searchPopupWidthPx, 700, 50, 10000, 5, builder,
                "Fixed width of popup in pixels",
                """ Use a fixed size popup. This sets the width of the popup in pixels irrespective of the screen or ide size""".trimIndent())
        keeper.createJBIntSpinnerComponent(mSettings.file::searchPopupHeightPx, 700, 50, 10000, 5, builder,
                "Fixed height of popup in pixels", """
                    Use a fixed size popup. This sets the height of the popup in pixels irrespective of the screen or ide size""".trimIndent())
        keeper.createJSpinnerComponent(mSettings.file::horizontalPositionOnScreen, 0.5, 0.1, 1.0, 0.01, builder,
            "X Position of search area on screen", """Relative X position on screen. 0 means all the way left, 1 means all the way right""".trimIndent())
        keeper.createJSpinnerComponent(mSettings.file::verticalPositionOnScreen, 0.5, 0.1, 1.0, 0.01, builder,
            "Y Position of search area on screen", """Relative Y position on screen. 0 means all the way left, 1 means all the way right""".trimIndent())
        keeper.createJBIntSpinnerComponent(mSettings.file::searchBarHeight, 30, 10, 300, 1, builder,
                "Height of the search bar in pixels", """""".trimIndent())
        keeper.createJBIntSpinnerComponent(mSettings.file::searchItemHeight, 30, 10, 100, 1, builder,
            "Height of the search items in pixels", """""".trimIndent())
        keeper.createCheckboxComponent(mSettings.file::showFileIcon, builder, "Show file icon",
            """""".trimIndent())
        keeper.createCheckboxComponent(mSettings.file::showNumberInSearchView, builder, "Show the index of each item in search view",
                """If checked show the number (index) of the item in the view as a number in front of the result """.trimIndent())
        keeper.createCheckboxComponent(mSettings.file::shrinkViewDynamically, builder, "Shrink the search area to only the found results",
                """If checked the search area will shrink to the number of results. Else the search area height
                will always be the configured height""".trimIndent())
        keeper.createCheckboxComponent(mSettings.file::showEditorPreview, builder, "Show editor preview",
                """ If checked, a small editor will be shown with the selected file contents. Can be used to quickly 
                    edit files. May negatively impact the performance. If selected, shrinking the search box is not supported""".trimIndent())
        keeper.createComboboxComponent(mSettings.file::editorPreviewLocation, EditorLocation.values(), builder, "Location of the editor preview",
            """Show the preview editor either below or to the right of the search box""".trimIndent())
        keeper.createJBIntSpinnerComponent(mSettings.file::minSizeEditorPx, 200, 40, 5000, 5, builder,
                "Min size of the editor view in pixels", """Minimum size of the editor in pixels. 
                    If the popup size is scaled with the size of the editor and the editor size is below this value, the editor is hidden. """.trimIndent())
        keeper.createJSpinnerComponent(mSettings.file::editorSizeRatio, 0.5, 0.1, 1.0, 0.01, builder, "Editor preview ratio", """
                    The ratio of the preview editor size as a fraction of the total width or height of the popup. 
                    If the preview editor is shown below the search area, the fraction of the total height will be selected.
                    If the preview editor is shown to the right of the search area, the fraction of the total width will be selected""".trimIndent())
        keeper.createCheckboxComponent(mSettings.file::searchMultiThreaded, builder, "Testoption, search multithreaded","")

            // Create Relative file opening actions
        builder.addSeparator()
            .addComponent(warningText)

        keeper.createActionsTableComponent(mSettings::openRelativeFileActions, builder, "Create action for opening file", """
                Open a file that is related to the currently open file. If no regex is entered, %name% is set to the name of the current file (without extension).
                If not empty, %rname% is set to the name of the file that matches the regex that is closest to the currently open file (without extension).
                The action to open the file starts from the reference file directory, so enter a relative path. 
                The %cname% variable is set to the name of the currently open file. This name is compared with the files in the open path.
                If a file in the directory matches partly, it is considered to be the same 
                (if the current filename is MyFileTest it will open the file MyFile unless MyFileTest also exists in the open path).
                Note that %rname% and %cname% cannot be used at the same time. If you want to have multiple options use the | to split them. The options
                are evaluated in order""".trimIndent(), arrayOf("Name", "Reference file", "Open path", "Shortcut"),
            arrayOf("MyActionName", "Regex", "src/%rname%Test.cc", "alt shift P"), arrayOf(1, 1, 1, 1),
            mSettings, 0, 3, ::registerOpenRelativeFileActions)

        // Create relative search actions table
        builder.addSeparator()
        keeper.createActionsTableComponent(mSettings::searchRelativeFileActions, builder, "Create action for searching files related to relative path", """
                Search in all files next to or below the file satisfying regex closest to the open file. If no regex is entered, the location of the
                open file is used. For example: Use CmakeList.txt as reference, search action will search all files in the same and lower directories of the 
                folder containing this file. Note that 'closest' means closest up the file tree, it does not look down. If no file satisfying the regex is found, 
                the search directory is set to the directory at the maximum search distance. If no regex is entered, %name% is set to the name of the current file (without extension).
                """.trimIndent(), arrayOf("Name", "Path", "Extensions", "Shortcut"), arrayOf("ActionName", "/", ".txt, .md", "alt shift H"), arrayOf(2, 2, 1, 2),
            mSettings, 0, 3, ::registerSearchRelativeFileActions)

        // Create search files matching pattern actions table
        builder.addSeparator()
        keeper.createActionsTableComponent(mSettings::searchFilesMatchingPatterActions, builder, "Search for files matching pattern", """
                    Search through all files where the filename matches a regex""".trimIndent(), arrayOf("MyActionName", "/", "Regex", "alt ctrl P"),
                    arrayOf("MyActionName", "Regex", "h", "alt shift P"), arrayOf(2, 3, 1, 2),
                    mSettings, 0, 3, ::registerSearchFileMatchingPatternActions)
        // Create file in path search actions
        builder.addSeparator()
        keeper.createActionsTableComponent(mSettings::searchPathActions, builder, "Search in path", """
               Search in a path. Use / as first character searches in the folder that is open in the editor. 
               Start with . to create a relative path (./ searches in directory of currently open file ../ in its parent etc.)
               Specify the extensions you want to search for, if empty all are included.
                """.trimIndent(), arrayOf("Name", "Path", "Extensions", "Shortcut"), arrayOf("ActionName", "/", ".cc,.cpp", "alt shift V"),
                arrayOf(2, 1, 3, 2), mSettings, 0, 3, ::registerSearchFileInPathActions)

        keeper.createActionsTableComponent(mSettings::searchRecentFilesActions, builder, "Search in most recently opened files", """
                Search through all the files that you have opened most recently. Also includes all files that are currently open in your editor.
                If the extension is empty, include all""".trimIndent(), arrayOf("Name", "History length", "Extensions", "Shortcut"),
                arrayOf("SearchRecentFiles", "10", ".txt,.md", "alt shift R"), arrayOf(2, 1, 1, 2),
            mSettings, 0, 3, ::registerSearchRecentFiles)
        keeper.createActionsTableComponent(mSettings::searchOpenFilesActions, builder, "Search in all files currently open in editor", """
                Search through files currently open in your editor with extension. If extension is empty, include all """.trimIndent(),
                arrayOf("Name", "Extensions", "Shortcut"), arrayOf("SearchOpenFiles", ".txt,.md", ""), arrayOf(2, 1, 2),
                mSettings, 0, 2, ::registerSearchOpenFiles)
        keeper.createActionsTableComponent(mSettings::searchAllFilesActions, builder, "Search in all files, including files that are not tracked by version control",
            """Special action to search through all files""".trimIndent(),arrayOf("Name", "Extensions", "Shortcut"), arrayOf("SearchAllFiles", "", "alt shift O"),
            arrayOf(2, 1, 2), mSettings, 0, 2, ::registerSearchAllFiles)

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
}


