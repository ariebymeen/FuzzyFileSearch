package com.fuzzyfilesearch.actions
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.util.preferredHeight
import com.intellij.ui.util.preferredWidth
import java.awt.BorderLayout
import java.awt.Point
import javax.swing.*

class TipsDialogView(project: Project?) : DialogWrapper(project) {
    enum class HelpType {
        OPEN_RELATIVE_FILE,
        SEARCH_RELATIVE_PATH,
        SEARCH_IN_PATH,
        SEARCH_FILES_MATCHING_PATTERN
    }

    val mHeaderBodyPanel = JPanel(BorderLayout())
    val mainViewPanel = JPanel(BorderLayout())
    var mInformationPanel = JEditorPane()
    val mBodyScrollPane = JBScrollPane(mInformationPanel)
    val mHelpSelectorDropdown = ComboBox(HelpType.values())

    init {
        title = "FuzzyFileSearch Help"
        init()
    }

    override fun createCenterPanel(): JComponent {
        mInformationPanel.isEditable = false
        mInformationPanel.contentType = "text/html"

        val header = JPanel(BorderLayout())

        mHelpSelectorDropdown.preferredHeight = 40
        mHelpSelectorDropdown.addActionListener{
             showHelpType(mHelpSelectorDropdown.getSelectedItem() as HelpType)
        }

        mainViewPanel.border = null
        mHeaderBodyPanel.border = null
        header.border = null
        header.add(mHelpSelectorDropdown, BorderLayout.EAST)

        mHeaderBodyPanel.add(header, BorderLayout.NORTH)
        mHeaderBodyPanel.add(mainViewPanel, BorderLayout.CENTER)

        mBodyScrollPane.border = BorderFactory.createEmptyBorder()
        mBodyScrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        mainViewPanel.add(mBodyScrollPane, BorderLayout.CENTER)

        // Set the initial width of the popup
        val fm = mInformationPanel.getFontMetrics(mInformationPanel.font)
        mHeaderBodyPanel.preferredWidth = 90 * fm.charWidth('a') // make popup at least 100 chars wide
        mHeaderBodyPanel.preferredHeight = 90 * fm.charWidth('a')
        mainViewPanel.preferredHeight = 80 * fm.charWidth('a')

        // Initially fill the information label
        showHelpType(mHelpSelectorDropdown.getSelectedItem() as HelpType)

        return mHeaderBodyPanel
    }

    fun showHelpType(selectedItem: HelpType) {
        when (selectedItem) {
            HelpType.OPEN_RELATIVE_FILE -> {
                mInformationPanel.text = """
                   <html>
                        <h2>Open relative file</h2>
                        <p>
                            The open relative file actions can be used to quickly open files that are related to the currently open file.
                            This can be especially useful when your project has a predictable structure that can be used to switch between files.
                            This can be used to switch between source and header files, or source and test files for example.
                        </p>
                        <p>
                             
                            <b>Name:</b> This is the name of the action (intelij ide concept). This can be used for ideavim integration.
                            To validate that the action is registered and to test the action, by searching for the action in Help->Find Action you can 
                            manually trigger the action. 
                            <br> The actions are registered with the ide as <i>com.fuzzyfilesearch.ActionName</i> 
                            <br> Action with name 'MyCustomAction' is registered as com.fuzzyfilesearch.MyCustomAction. <br>
                            
                            <br> <b>Reference File:</b> Optional regex that matches a filename (without path). If no regex is entered, the 
                            directory of the currently opened file is used as the reference directory, from which the other file is searched. 
                            If filled, enter a regex (of filename e.g. CMakeLists.txt) to match with a file. When the action is triggered,
                            a file is searched where the filename satisfies the regex. First the files in the current directory (of current open file), 
                            then the upper directories are explored until the project path is reached. If a matching file is found, the directory of this
                            file is used as reference path. This is very useful when the project has a predictable structure with 
                            reference files, e.g. a CMakeLists.txt file. <br>
                            
                            <br> <b>Open path:</b> This is the pattern of the file to open. There are two main options for finding the correct filename, <i>rname</i> and <i>cname</i>. 
                            The open path is a sort of template that is matched against the open files. The <i>rname</i> is the filename (without extension) of the reference file
                            that matches the regex. This can only be used when the regex is filled in. The <i>cname</i> is the name of the currently open file This is the pattern of the file to open. There are two main options for finding the correct filename, rname and cname. 
                            The open path is a sort of template that is matched against the open files. The <i>rname</i> is the filename (without extension) of the reference file
                            that matches the regex. This can only be used when the regex is filled in. The <i>cname</i> is the name of the currently open file. <br><br>
                            Using <i>rname</i>: When using the rname in the template (e.g. src/%rname%.kt) %rname% will be simply replaced with 
                            the found reference file name (without extension). The reference location is the directory in which the reference file is found.
                            This file is opened, when it is not found, an error message is shown. <br>
                            Using <i>cname</i>: Using cname is slightly more flexible. This can be used both in combination with a reference regex or none. 
                            Instead of simply replacing %cname% in the pattern with the name of the currently open file, the pattern is compared against both the current 
                            file name and a file on disk to find a match. The pattern matches when at least part of the filename is equal to the current filename
                            and is matching the pattern. To find the file, all lower directories are also searched until a match is found.
                            This means that it might not always find the best fit if there are ambiguous files. <br>
                            
                            <br> <b>Shortcut:</b>This is an optional shortcut field where you can register a shortcut to trigger the action. 
                            Note that this can also be done by using the keymap in the ide settings. When registering a shortcut that is already
                            registered, the shortcut will not work. Find the shortcut in the keymap and remove the ambiguity. 
                        </p>
                        <h3>Example 1</h3>
                        <p>
                            Given the following file structure: <br>
                            
                            | MyFiles <br>
                            | -- src <br>
                            | -- | -- MyImplementation.kt <br>
                            | -- view <br>
                            | -- | -- MyImplementationView.kt <br>
                            | -- test <br>
                            | -- | -- MyImplementationTest.kt <br>
                            | -- gradle.properties <br>
                            
                            <br> To open MyImplementationView.kt from any of the other files, you could use the following:
                            <br> Option 1:
                            <br> Name: OpenView | Reference file: gradle.properties | Open path: view/%cname%View.kt
                            <br> Name: OpenSrc  | Reference file: gradle.properties | Open path: src/%cname%.kt
                            <br> 
                            <br> Option 2:
                            <br> Name: OpenView | Reference file:  | Open path: ../view/%cname%View.kt
                            <br> Name: OpenSrc  | Reference file:  | Open path: ../src/%cname%.kt
                            <br> 
                            <br> Note that if view/ would also contain a file MyImplView.kt, the resulting template is ambiguous and can lead to unexpected behaviour.
                            In this example %cname% will become MyImplementation, even when the current file is MyImplementationTest.kt
                        </p>
                        <h3>Example 2</h3>
                        <p>
                            Given the following file structure: <br>
                            
                            | MyFiles <br>
                            | -- src <br>
                            | -- | -- MyImplementation.kt <br>
                            | -- view <br>
                            | -- | -- MyImplementationView.kt <br>
                            | -- test <br>
                            | -- | -- MyImplementationTest.kt <br>
                            | -- MyImplementation.definitions <br>
                            
                            <br> To open MyImplementationView.kt & test from any of the other files, you could use the following:
                            <br> Name: OpenView | Reference file: gradle.properties | Open path: view/%rname%View.kt
                            <br> Name: OpenSrc  | Reference file: gradle.properties | Open path: test/%rname%Test.kt
                            <br> 
                            <br> In this case %rname% equates to MyImplementation.
                        </p>
                        <h3>Example 3</h3>
                        <p>
                            Given the following file structure: <br>
                            
                            | MyFiles <br>
                            | -- src <br>
                            | -- | -- MyImplementation.kt <br>
                            | -- | -- helpers <br>
                            | -- | -- | -- MyImplementationHelper.kt <br>
                            | -- view <br>
                            | -- | -- MyImplementationView.kt <br>
                            | -- test <br>
                            | -- | -- MyImplementationTest.kt <br>
                            | -- | -- helpers <br>
                            | -- | -- | -- MyImplementationHelperTest.kt <br>
                            | -- MyImplementation.definitions <br>
                            
                            <br> To open MyImplementationHelper.kt & test from any of the other files, you could use the following:
                            <br> Name: OpenHelper | Reference file: gradle.properties | Open path: src/helpers/%rname%Helper.kt
                            <br> 
                            <br> If you just want to be able to switch between the helper implementation and the test:
                            <br> Name: OpenHelperTest   | Reference file: gradle.properties | Open path: src/%cname%Helper.kt
                            <br> Name: OpenHelperSource | Reference file: gradle.properties | Open path: test/%cname%HelperTest.kt
                            <br> 
                            <br> In this case %cname% equates to helpers/MyImplementation 
                        </p>
                    </html>
                """.trimIndent()
            }
            HelpType.SEARCH_IN_PATH -> {
                mInformationPanel.text = """
                   <html>
                        <h2>Search in path</h2>
                        <p>
                            The search in path action can be used to search for all files in a given directory. Allowing to assign 
                            shortcuts to different paths. By settings extensions, the files can be filtered to contain a set of extensions.
                        </p> 
                        <p>
                            <b>Name:</b> This is the name of the action. This can be used for ideavim integration.
                            To validate that the action is registered and to test the action, by searching for the action in Help->Find Action you can 
                            manually trigger the action. 
                            <br> The actions are registered with the ide as <i>com.fuzzyfilesearch.ActionName</i> <br> 
                            <br> Action with name 'MyCustomAction' is registered as com.fuzzyfilesearch.MyCustomAction. <br>
                            
                            <br> <b>Path:</b> The path to search for files. If the path is left empty, the directory of the currently open file is searched.
                            If the path starts with /, the search directory is relative to the project root. When a project is loaded, this is the project root.
                            If a directory is loaded, but no project, this directory becomes the root. E.g. if the path is /src,
                            the search directory becomes <PROJECT_ROOT>/src. If the path start with a '.', the path relative to the currently open file  is used as 
                            base directory. E.g. ../../ searches 2 through all files 2 directories up from the currently edited file. <br>
                            
                            <br> <b>Extensions:</b> Optionally give a list of extensions to filter. If empty, all file extensions are included in the search.
                            Multiple extensions can be entered, separated by '|' or ','. The . in front of the extension is optional. 
                            To search for all txt and md files: ".txt|.md" or "txt|md|. <br>
                            
                            <br> <b>Shortcut:</b> This is an optional shortcut field where you can register a shortcut to trigger the action. 
                            Note that this can also be done by using the keymap in the ide settings. When registering a shortcut that is already
                            registered, the shortcut will not work. Find the shortcut in the keymap and remove the ambiguity. 
                        </p>
                        <h3>Example</h3>
                        <p>
                            Given the following file structure: <br>
                            
                            | MyFiles <br>
                            | -- src <br>
                            | -- | -- MyImplementation.kt <br>
                            | -- | -- helpers <br>
                            | -- | -- | -- MyImplementationHelper.kt <br>
                            | -- view <br>
                            | -- | -- MyImplementationView.kt <br>
                            | -- test <br>
                            | -- | -- MyImplementationTest.kt <br>
                            | -- | -- helpers <br>
                            | -- | -- | -- MyImplementationHelperTest.kt <br>
                            | -- MyImplementation.definitions <br>
                            
                            <br>To open MyImplementationHelper.kt & test from any of the other files, you could use the following:
                            <br>Name: OpenHelper | Reference file: gradle.properties | Open path: src/helpers/%rname%Helper.kt
                            <br>
                            <br>If you just want to be able to switch between the helper implementation and the test:
                            <br>Name: OpenHelperTest   | Reference file: gradle.properties | Open path: src/%cname%Helper.kt
                            <br>Name: OpenHelperSource | Reference file: gradle.properties | Open path: test/%cname%HelperTest.kt
                            <br>
                            <br>In this case %cname% equates to helpers/MyImplementation 
                        </p>
                    </html>
                """.trimIndent()
            }
            HelpType.SEARCH_RELATIVE_PATH -> {
                mInformationPanel.text = """
                   <html>
                        <h2>Search relative path</h2>
                        <p>
                            The search relative path actions can be used to search in a directory containing a special marker file. This could be 
                            a cmake file, or any other recognisable file. This can be useful to search in files that are somehow related to the file currently 
                            begin edited.
                        </p>
                        <p>
                            <b>Name:</b> This is the name of the action (intelij ide concept). This can be used for ideavim integration.
                            To validate that the action is registered and to test the action, by searching for the action in Help->Find Action you can 
                            manually trigger the action. 
                            <br> The actions are registered with the ide as <i>com.fuzzyfilesearch.ActionName</i> <br> 
                            <br> Action with name 'MyCustomAction' is registered as com.fuzzyfilesearch.MyCustomAction. <br>
                            
                            <br><b>Reference File:</b> Regex that matches a filename (without path). 
                            When the action is triggered, a file is searched where the filename satisfies the regex. 
                            First the files in the current directory (of current open file), then the upper directories are explored until the project path is reached.
                            If a matching file is found, the directory of this file is used as reference path.
                            This is very useful when the project has a predictable structure with reference files, e.g. a CMakeLists.txt file. <br>
                            
                            <br><b>Extensions:</b> Optionally give a list of extensions to filter. If empty, all file extensions are included in the search.
                            Multiple extensions can be entered, separated by '|' or ','. The . in front of the extension is optional. 
                            
                            <b>Shortcut:</b> This is an optional shortcut field where you can register a shortcut to trigger the action. 
                        </p>
                        <h3>Example</h3>
                        <p>
                            Given the following file structure: <br>
                            
                            | MyFiles <br>
                            | -- src <br>
                            | -- | -- MyImplementation.kt <br>
                            | -- | -- helpers <br>
                            | -- | -- | -- MyImplementationHelper.definitions <br>
                            | -- | -- | -- MyImplementationHelper.kt <br>
                            | -- view <br>
                            | -- | -- MyImplementationView.kt <br>
                            | -- test <br>
                            | -- | -- MyImplementationTest.kt <br>
                            | -- | -- helpers <br>
                            | -- | -- | -- MyImplementationHelperTest.kt <br>
                            | -- MyImplementation.definitions <br>
                            
                            <br>To search in the directory containing a *.definitions file, the following action can be specified:
                            <br>Name: SearchFromDefinition | Reference file: [A-Za-z]+.definitions | Extensions: | Shortcut: 
                            <br>
                            <br>Triggering the action when one of the files is opened will search in the MyFiles directory. Only when the action is triggered when
                            the MyImplementationHelper.kt or the MyImplementationHelper.definitions are open, the search directory becomes
                            MyFiles/src/helpers
                        </p>
                    </html>
                """.trimIndent()

            }
            HelpType.SEARCH_FILES_MATCHING_PATTERN -> {
                mInformationPanel.text = """
                   <html>
                        <h2>Search files with pattern</h2>
                        <p>
                            This action can be used to search for files where the filename satisfies a particular regex. 
                        </p>
                        <p>
                            <b>Name:</b> This is the name of the action. This can be used for ideavim integration.
                            To validate that the action is registered and to test the action, by searching for the action in Help->Find Action you can 
                            manually trigger the action. 
                            <br> The actions are registered with the ide as <i>com.fuzzyfilesearch.ActionName</i> <br> 
                            <br> Action with name 'MyCustomAction' is registered as com.fuzzyfilesearch.MyCustomAction. <br>
                            <br>
                            <br><b>Path:</b> The path to search for files. If the path is left empty, the directory of the currently open file is searched.
                            If the path starts with /, the search directory is relative to the project root. When a project is loaded, this is the project root.
                            If a directory is loaded, but no project, this directory becomes the root. E.g. if the path is /src,
                            the search directory becomes <PROJECT_ROOT>/src. If the path start with a '.', the path relative to the currently open file  is used as 
                            base directory. E.g. ../../ searches 2 through all files 2 directories up from the currently edited file. 
                            <br>
                            <br><b>Pattern:</b> Regex to which all files in Path are matched. Note that only the filename (with extension) is matched, not
                            the file path. Only a single regex can be entered. 
                            <br>
                            <br><b>Shortcut:</b> This is an optional shortcut field where you can register a shortcut to trigger the action. 
                            Note that this can also be done by using the keymap in the ide settings. When registering a shortcut that is already
                            registered, the shortcut will not work. Find the shortcut in the keymap and remove the ambiguity. 
                        <p>
                    </html>
                """.trimIndent()

            }
        }

        mInformationPanel.revalidate()
        mInformationPanel.repaint()
        mBodyScrollPane.revalidate()
        mBodyScrollPane.repaint()
        mHeaderBodyPanel.revalidate()
        mHeaderBodyPanel.repaint()
        SwingUtilities.invokeLater {
            mBodyScrollPane.viewport.viewPosition = Point(0, 0)
        }
    }
}
