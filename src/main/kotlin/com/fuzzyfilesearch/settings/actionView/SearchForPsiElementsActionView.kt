package com.fuzzyfilesearch.settings.actionView

import com.fuzzyfilesearch.actions.ActionType
import com.fuzzyfilesearch.actions.RegexMatchInFiles
import com.fuzzyfilesearch.actions.SearchForPsiElementsAction
import com.fuzzyfilesearch.actions.utils
import com.fuzzyfilesearch.components.LabeledTextField
import com.fuzzyfilesearch.components.WrappedCheckbox
import com.fuzzyfilesearch.settings.verifyActionName
import com.fuzzyfilesearch.settings.verifyRegex
import com.fuzzyfilesearch.settings.verifyShortcut
import javax.swing.JPanel

class SearchForPsiElementsActionView() : ActionViewBase() {

    val pathField = LabeledTextField(
        "Path: ",
        "Enter a path. Starting with '/' searches from the project root, starting with '.' searches from the current file. Leaving it empty searches only the current file",
        "/")
    val psiTypeFilterField  = LabeledTextField(
                "Code element Type: ",
                "<b>Select the types of code elements to search</b><br>" +
                "Filter on specific PSI (Program Structure Interface) elements to search for things like methods, classes etc. " +
                "Provide a comma separated list of PSI elements to search for")
    val displayFromField = LabeledTextField(
        "Display from: ",
        "Display the text from this string in the search view" +
        "Example:<br> entering '::' will display the bold text: void <i>my_class::</i><b>my_method()</b>")
    val displayToField = LabeledTextField(
        "Display to: ",
        "Display the text to this string in the search view" +
        "Example:<br> entering '{' will display the bold text: void <b>my_method() </b>{ return \"my_method\"; }")
    val extensionFilterField =
            LabeledTextField(
                "Extensions filter: ",
                "Enter a comma seperated list of extensions to filter (e.g. '.kt,.java'). The files with these extensions will be searched for PSI elements")
    val vcsTrackedOnlyCheckbox = WrappedCheckbox(
        "Only search files tracked by vcs",
        "If selected, only search files that are tracked by vcs")

    override fun addToPanel(panel: JPanel) {
        panel.add(actionNameField)
        panel.add(pathField)
        panel.add(psiTypeFilterField)
        panel.add(displayFromField)
        panel.add(displayToField)
        panel.add(extensionFilterField)
        panel.add(shortcutField)
        panel.add(vcsTrackedOnlyCheckbox)
    }

    override fun initialize(settings: utils.ActionSettings) {
        actionNameField.textField.text = settings.name
        shortcutField.textField.text = settings.shortcut

        val custom = SearchForPsiElementsAction.parseSettings(settings.generic)
        pathField.textField.text = custom.path
        psiTypeFilterField.textField.text = if (custom.psiElementTypes.isEmpty()) "" else custom.psiElementTypes.joinToString(", ")
        displayFromField.textField.text = custom.displayFrom
        displayToField.textField.text = custom.displayTo
        extensionFilterField.textField.text = if (custom.extensionList.isEmpty()) "" else custom.extensionList.joinToString(", ")
        vcsTrackedOnlyCheckbox.box.isSelected = custom.onlyVcsTracked

        initialSettings = this.getStored()
    }

    override fun modified(): Boolean {
        return !utils.isEqual(initialSettings, this.getStored())
    }

    override fun verify(): String {
        var res = verifyActionName(actionNameField.text())
        if (res.isNotEmpty()) return res

        res = if (psiTypeFilterField.text().isBlank()) "Please provide at least one PSI type to filter" else ""
        if (res.isNotEmpty()) return res

        val shortcut = shortcutField.text().trim()
        return verifyShortcut(shortcut)
    }

    override fun getStored(): Array<String> {
        return arrayOf(
            ActionType.SEARCH_FOR_CODE_ELEMENTS.toString(),
            actionNameField.text(),
            shortcutField.text(),
            pathField.text(),
            extensionFilterField.text(),
            vcsTrackedOnlyCheckbox.box.isSelected.toString(),
            psiTypeFilterField.text(),
            displayFromField.text(),
            displayToField.text(),
            )
    }

    override fun help(): String {
        return """
            <b>Opens a search view to search through all code elements that match one of the provided filters</b><br>
            PSI elements are the building blocks of your code — essentially any meaningful part of a file, like functions, classes, methods, variables, or statements.
            This action provides a way to find and search through all code elements of a specific type. Because the type of elements in the PSI tree
            is different for all languages, you can provide the type yourself. 
            <br>
            <br>
            Some types:<br>
            
            <b>Kotlin:</b> <br>
            <ul>
            <li><b>KtClass:</b> Matches all classes</li>
            <li><b>FUN:</b> Matches all methods</li>
            </ul>
            <br><br>
            <b>C++:</b> <br>
            <lu>
            <li><b>OCStruct:</b> Matches all structs and classes</li>
            <li><b>OCFunctionDeclaration:</b> Matches all function declarations</li>
            </ul>
            <br><br>
            Other types can be found in the intellij docs (Or go to 'Find Action' and search for PrintPsiElementTypes. This will print the code element types to the console).
            <br>
            This action does: <br>
            First: search for all files in the specified path, for files (tracked by vcs if checked) with the specified extension.<br>
            Then : for all files matching the path and extension, find all psi elements that match the filter<br>
            <br>
            <b>Name:</b> Enter a unique name, this is used to register the action in the intellij framework. The action will be registered as <i>com.fuzzyfilesearch.%NAME%</i><br>
            <b>Path (optional):</b> Enter a path, where '/' is the project root. To search relative to the open file, start with './'. If empty, only search through the current file<br>
            <b>Type:</b> Provide a comma separated list of code elements to search for, e.g KTMethod to find all kotlin methods<br>
            <b>Display from:</b> Display the text from this string in the search view. Example:<br> entering '::' will display the bold text: void <i>my_class::</i><b>my_method()</b><br>
            <b>Display to:</b> Display the text to this string in the search view. Example: entering '{' will display the bold text: void <b>my_method() </b>{ return \"my_method\"; }<br>
            <b>Extensions filter (optional):</b> List the extensions to search through, seperated by ','. If empty, all files in the (recursive) directory are included in the search. <br>
            <b>Shortcut (optional):</b> Enter a shortcut to trigger the action <br>
        """.trimIndent()
    }
}
