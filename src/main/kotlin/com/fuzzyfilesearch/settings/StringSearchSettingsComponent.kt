package com.fuzzyfilesearch.settings

import com.fuzzyfilesearch.actions.*
import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel

class StringSearchSettingsComponent(val mSettings: GlobalSettings.SettingsState) {
    var panel: JPanel
    var keeper = SettingsComponentKeeper()
    var warningText = createWarningLabel()

    init {
        val builder = FormBuilder()
            .addComponent(JBLabel("<html><strong>Settings for string search</strong></html>"))
            .addSeparator()

        keeper.createJBIntSpinnerComponent(mSettings.string::numberOfFilesInSearchView, 10, 1, 100, 1, builder,
            "Max number of files visible in search view", """
            Sets the maximum number of files in the search view. Defaults to 10, as this is the most that can be selected
            by number. Setting this value too high can seriously affect the rendering performance. Not that this only affects
            the number of files visible, all other files are still used in searching""".trimIndent())
        keeper.createComboboxComponent(mSettings.string::popupSizePolicy, PopupSizePolicy.values(), builder, "Popup scaling", """
               Select how the popup resizes. Fixed size will allow you to specify the size in pixels. Resize with
               ide bounds: specify the size of the popup as a fraction of the ide size. Resize with screen size: 
               specify the size of the popup as a fraction of the screen size. 
               This may give unexpected behaviour on multi-monitor setups""".trimIndent())
        keeper.createJSpinnerComponent(mSettings.string::searchPopupWidth, 0.3, 0.1, 1.0, 0.05, builder,
                "Search view width fraction", """The width of the search popup as a fraction of the screen width """.trimIndent())
        keeper.createJSpinnerComponent(mSettings.string::searchPopupHeight, 0.3, 0.1, 1.0, 0.05, builder,
            "Search view height fraction", """The width of the search popup as a fraction of the screen height""".trimIndent())
        keeper.createJBIntSpinnerComponent(mSettings.string::searchPopupWidthPx, 700, 50, 10000, 5, builder,
                "Fixed width of popup in pixels",
                """ Use a fixed size popup. This sets the width of the popup in pixels irrespective of the screen or ide size""".trimIndent())
        keeper.createJBIntSpinnerComponent(mSettings.string::searchPopupHeightPx, 700, 50, 10000, 5, builder,
                "Fixed height of popup in pixels", """
                    Use a fixed size popup. This sets the height of the popup in pixels irrespective of the screen or ide size""".trimIndent())
        keeper.createJSpinnerComponent(mSettings.string::horizontalPositionOnScreen, 0.5, 0.1, 1.0, 0.01, builder,
            "X Position of search area on screen", """Relative X position on screen. 0 means all the way left, 1 means all the way right""".trimIndent())
        keeper.createJSpinnerComponent(mSettings.string::verticalPositionOnScreen, 0.5, 0.1, 1.0, 0.01, builder,
            "Y Position of search area on screen", """Relative Y position on screen. 0 means all the way left, 1 means all the way right""".trimIndent())
        keeper.createJBIntSpinnerComponent(mSettings.string::searchBarHeight, 30, 10, 300, 1, builder,
                "Height of the search bar in pixels", """""".trimIndent())
        keeper.createJBIntSpinnerComponent(mSettings.string::searchItemHeight, 30, 10, 100, 1, builder,
            "Height of the search items in pixels", """""".trimIndent())
        keeper.createCheckboxComponent(mSettings.string::showFileIcon, builder, "Show file icon",
            """""".trimIndent())
        keeper.createCheckboxComponent(mSettings.string::showNumberInSearchView, builder, "Show the index of each item in search view",
                """If checked show the number (index) of the item in the view as a number in front of the result """.trimIndent())
        keeper.createCheckboxComponent(mSettings.string::shrinkViewDynamically, builder, "Shrink the search area to only the found results",
                """If checked the search area will shrink to the number of results. Else the search area height
                will always be the configured height""".trimIndent())
        keeper.createCheckboxComponent(mSettings.string::showEditorPreview, builder, "Show editor preview",
                """ If checked, a small editor will be shown with the selected file contents. Can be used to quickly 
                    edit files. May negatively impact the performance. If selected, shrinking the search box is not supported""".trimIndent())
        keeper.createComboboxComponent(mSettings.string::editorPreviewLocation, EditorLocation.values(), builder, "Location of the editor preview",
            """Show the preview editor either below or to the right of the search box""".trimIndent())
        keeper.createJBIntSpinnerComponent(mSettings.string::minSizeEditorPx, 200, 40, 5000, 5, builder,
                "Min size of the editor view in pixels", """Minimum size of the editor in pixels. 
                    If the popup size is scaled with the size of the editor and the editor size is below this value, the editor is hidden. """.trimIndent())
        keeper.createJSpinnerComponent(mSettings.string::editorSizeRatio, 0.5, 0.1, 1.0, 0.01, builder, "Editor preview ratio", """
                    The ratio of the preview editor size as a fraction of the total width or height of the popup. 
                    If the preview editor is shown below the search area, the fraction of the total height will be selected.
                    If the preview editor is shown to the right of the search area, the fraction of the total width will be selected""".trimIndent())
        keeper.createJBIntSpinnerComponent(mSettings::grepRememberPreviousQuerySeconds, 5, 0, 100, 1, builder, "Remember previous query (seconds)", """
                    Time (seconds) for which the previous query will be remembered. When zero, it is never remembered""".trimIndent())

            // Create Relative file opening actions
        builder.addSeparator()
            .addComponent(warningText)

        keeper.createCheckboxComponent(mSettings::applySyntaxHighlightingOnTextSearch, builder, "Apply syntax highlighting on text search", """
                    If checked, apply syntax highlighting on text search results. If false, plain text is used""".trimIndent())
        keeper.createComboboxComponent(mSettings::showFilenameForRegexMatch, ShowFilenamePolicy.values(), builder,
             "Show filename in regex search", """
                    Show filename in front of the matching string""".trimIndent())
        keeper.createCheckboxComponent(mSettings::useSelectedTextForGrepInFiles, builder, "Use selected text as initial query", """
                    If true, the selected text is used as initial query. If no text is selected, no query is set """.trimIndent())
        keeper.createActionsTableComponent(mSettings::searchStringMatchingPatternActions, builder, "Search for regex pattern in files", """
                    Search through all instances that match the pattern. If path starts with '/', search through all files. This may
                    be performance intensive if there are many files. If path is empty or '.' search only the current path. Else a relative path
                    is selected""".trimIndent(), arrayOf("Name", "Path", "Regex", "Shortcut", "Extension"),
                    arrayOf("MySearchAction", "", "Enter regex", "", ""), arrayOf(2, 1, 6, 2, 1), mSettings, 0, 3, ::registerSearchForRegexInFiles)

        keeper.createCheckboxComponent(mSettings::showFilenameForGrepInFiles, builder, "Show filename in grep result", """
                    Show filename in front of the matching string""".trimIndent())
        keeper.createActionsTableComponent(mSettings::searchStringMatchingSubstringActions, builder, "Grep in files", """
                    Search through all lines containing the substring. If path starts with '/', search through all files. This may
                    be performance intensive if there are many files. If path is empty or '.' search only the current path. Else a relative path
                    is selected""".trimIndent(), arrayOf("Name", "Path", "Extension", "Shortcut"),
            arrayOf("MySearchAction", "/", "", ""), arrayOf(1, 1, 1, 1), mSettings, 0, 3, ::registerGrepInFilesActions)

        builder.addSeparator()
            .addComponentFillVertically(JPanel(), 0)

        panel = builder.panel

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
