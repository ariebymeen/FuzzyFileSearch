package com.fuzzyfilesearch.settings

import com.intellij.ui.ColorPanel
import com.intellij.ui.FontComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel

class CommonSettingsComponent(val mSettings: GlobalSettings.SettingsState) {
    var panel: JPanel

    var keeper = SettingsComponentKeeper()

    var excludedDirs = JBTextArea()
    var fontSelectorDropdown = FontComboBox()
    var colorSelectorElement = ColorPanel()

    init {

        val builder = FormBuilder()
            .addComponent(JBLabel("<html><strong>Common settings for FuzzyFileSearch</strong></html>"))
            .addSeparator()

            .addComponent(createLabelWithDescription("Excluded folders", "Wildcards are not supported, enter the full name of the folder"))
            .addComponent(excludedDirs)

        keeper.createCheckboxComponent(mSettings.common::searchCaseSensitivity, builder, "Search case sensitive",
            "If checked the searching algorithm is case sensitive")
        keeper.createCheckboxComponent(mSettings.common::searchOnlyFilesTrackedByVersionControl, builder, "Search only files that are tracked by vcs",
            """ If checked only files that are tracked by a version control system (vcs) are searched.
                Else all files are part of the search (except for directories explicitly excluded)""".trimIndent())
        keeper.createComboboxComponent(mSettings.common::modifierKey, ModifierKey.values(),builder, "Modifier key", """
                    The results can be scrolled through using the arrow up/down keys or using the j/k key if the modifier
                    key is pressed. This key can be either the ctrl or the alt key.
                    The modifier key can also be used in combination with the number keys to quickly open a result, e.g. ctrl-1 to open result 1 (second result). 
                    This can be used as an alternative to scrolling to the result and pressing enter""".trimIndent())
        keeper.createTextFieldComponent(mSettings.common::openInVerticalSplit, builder, "Shortcut open file in vertical split", """
                    Set the shortcut for opening the selected file in a vertical split view""".trimIndent())
        keeper.createTextFieldComponent(mSettings.common::openInHorizontalSplit, builder, "Shortcut open file in horizontal split", """
                    Set the shortcut for opening the selected file in a horizontal split view""".trimIndent())
        keeper.createTextFieldComponent(mSettings.common::openInActiveEditor, builder, "Shortcut open file in aive editor", """
                    Apart from using enter, a custom shortcut can be added to open the currently selected file in the active editor""".trimIndent())
        keeper.createCheckboxComponent(mSettings.common::useDefaultFont, builder, "Use default popup font",
                """If checked use the same font as the editor, else a font can be selected""".trimIndent())
        builder.addLabeledComponent(
                createLabelWithDescription("Select popup font", """
                    Choose the font to be used in poupp
                """.trimIndent()), fontSelectorDropdown)
        keeper.createJBIntSpinnerComponent(mSettings.common::fontSize, 13, 1, 30, 1, builder, "Popup font size", """Choose the font size""")

        keeper.createCheckboxComponent(mSettings.common::useDefaultHighlightColor, builder, "Use default highlight color", "")
            builder.addLabeledComponent(
                createLabelWithDescription("Choose the highlight color", """
                    Choose the highlight color to be used in popup
                """.trimIndent()), colorSelectorElement)
        keeper.createCheckboxComponent(mSettings.common::openWithSingleClick, builder, "Open file with a single click", """
                    If checked, open the file in a single click. Opening the file in the preview is then only possible 
                    using the keyboard. If not clicked, the item must be double clicked to open the file""".trimIndent())
        keeper.createCheckboxComponent(mSettings.common::showTileInSearchView, builder, "Show tile in search view", """
                    If checked show the title at the top of the search view to signify the action that is done in the search view""".trimIndent())
        keeper.createJBIntSpinnerComponent(mSettings.common::titleFontSize, 9, 1, 30, 1, builder, "Popup title font size", """Choose the font size""")
        builder.addComponentFillVertically(JPanel(), 0)

        panel = builder.panel

    }
}


