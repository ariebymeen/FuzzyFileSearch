package com.fuzzyfilesearch.settings

import com.fuzzyfilesearch.actions.ActionType
import com.fuzzyfilesearch.actions.utils
import com.fuzzyfilesearch.settings.actionView.ActionViewWrapper
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.*
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileWrapper
import com.jetbrains.rd.util.remove
import java.awt.Component
import java.io.IOException
import java.util.regex.PatternSyntaxException
import javax.swing.JPanel
import javax.swing.KeyStroke

fun <T> List<T>.findDuplicateElements(): List<T> {
    return this.groupBy { it } // Group by the element itself
        .filter { it.value.size > 1 } // Keep only groups with more than one element
        .keys // Get the keys (the duplicate elements) from these groups
        .toList() // Convert the set of keys to a list
}

fun verifyActionName(name: String): String {
    if (name.isEmpty()) {
        return "Please provide a (unique) name"
    }

    if (name.trim().contains(' ')) {
        return "Please provide a name without spaces"
    }
    return ""
}

fun verifyRegex(pattern: String): String {
    if (pattern.isEmpty()) {
        return "Please provide a pattern"
    }
    try {
        val regex = Regex(
            pattern = pattern,
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        return ""
    } catch (e: PatternSyntaxException) {
        return "Parsing regex failed with error ${e.message!!}"
    }
}

fun verifyShortcut(shortcut: String): String {
    if (shortcut.isNotEmpty()) {
        val keyStroke = KeyStroke.getKeyStroke(shortcut)
        if (keyStroke == null) {
            return "Shortcut '$shortcut' is not a valid shortcut. Please provide something similar to 'alt shift U'"
        }
        val actions = KeymapManager.getInstance().activeKeymap.getActionIds(keyStroke, null)
        val error = actions.any { !it.startsWith("com.fuzzyfilesearch.") }
        if (error) {
            return "Error, shortcut $shortcut is already in use with [${actions.joinToString(",")}].\nGo to keymap settings to remove this shortcut"
        }
    }
    return ""
}

fun addActionConfigsToPanel(actionSettings: Array<Array<String>>, panel: JPanel, types: Array<ActionType>) {
    actionSettings.forEach { action ->
        val settings = utils.getGenericActionSettings(action)
        if (types.contains(settings.type)) {
            val comp = ActionViewWrapper(panel, types)
            comp.initializeFromSettings(settings)
            panel.add(comp)
        }
    }
}

/** Filter all stored settings and return all entries where the type is in the types array */
fun getActionSettingsForTypes(
    settings: GlobalSettings.SettingsState,
    types: Array<ActionType>): List<Array<String>> {
    return settings.allActions.filter { action -> types.contains(ActionType.valueOf(action[0])) }
}

/** Clear all entries satisfying the type array from the settings and remove the action registration */
fun clearSettingsAndClearActionRegistrationForTypes(
    settings: GlobalSettings.SettingsState,
    types: Array<ActionType>) {
    val toRemove = getActionSettingsForTypes(settings, types)
    toRemove.forEach { action ->
        val actionSettings = utils.getGenericActionSettings(action)
        utils.unregisterAction(actionSettings.name, actionSettings.shortcut)
        settings.allActions = settings.allActions.remove(action)
    }
}

/** Update settings and register new actions */
fun addSettingsAndRegisterActions(
    settings: GlobalSettings.SettingsState,
    toAdd: List<Array<String>>) {

    val current = settings.allActions.toMutableList()
    current.addAll(toAdd)
    settings.allActions = current.toTypedArray()

    utils.registerActionsFromSettings(toAdd.toTypedArray(), settings)
}

fun exportActionsToFile(settings: GlobalSettings.SettingsState, types: Array<ActionType>) {
    val jsonData: String = Gson().toJson(getActionSettingsForTypes(settings, types))

    // Open a file chooser where the user can specify the file name
    val descriptor = FileSaverDescriptor("Save JSON File", "Choose a location to save")
    val dialog: FileSaverDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, null)
    val fileWrapper: VirtualFileWrapper? = dialog.save(null as VirtualFile?, null)

    fileWrapper?.file?.let { file ->
        ApplicationManager.getApplication().runWriteAction {
            try {
                file.writeText(jsonData)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}

fun importFromFile(settings: GlobalSettings.SettingsState) {
    val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
    val file = FileChooser.chooseFile(descriptor, null, null) ?: return

    try {
        val content = VfsUtil.loadText(file)
        val gson = Gson()
        val listType = object : TypeToken<Array<Array<String>>>() {}.type
        val imported = gson.fromJson(content, listType) as Array<Array<String>>
        val existingActionNames = settings.allActions.map { it[1] }
        val toAdd = imported.filter { action ->
            !existingActionNames.contains(utils.getGenericActionSettings(action).name)
        }
        addSettingsAndRegisterActions(settings, toAdd)
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return
}

fun getShortcuts(components: Array<Component>): List<String> {
    return components.map { it -> (it as ActionViewWrapper).getShortcut() }
}

fun getActionNames(components: Array<Component>): List<String> {
    return components.map { it -> (it as ActionViewWrapper).getActionName() }
}

fun getActionNamesForOtherActionTypes(
    settings: GlobalSettings.SettingsState,
    actionTypes: Array<ActionType>): List<String> {
    val otherActionTypes = ActionType.values().toMutableList()
    otherActionTypes.removeAll(actionTypes)
    return getActionSettingsForTypes(settings, otherActionTypes.toTypedArray()).map { it[1] }
}

fun getShortcutsForOtherActionTypes(
    settings: GlobalSettings.SettingsState,
    actionTypes: Array<ActionType>): List<String> {
    val otherActionTypes = ActionType.values().toMutableList()
    otherActionTypes.removeAll(actionTypes)
    return getActionSettingsForTypes(settings, otherActionTypes.toTypedArray()).map { it[2] }
}

fun setWarningForDuplicateShortcuts(
    settings: GlobalSettings.SettingsState,
    components: Array<Component>,
    actionTypes: Array<ActionType>) {
    val allShortcuts = getShortcutsForOtherActionTypes(settings, actionTypes) + getShortcuts(components)
    val duplShortcuts = allShortcuts.findDuplicateElements().filter { it -> it.isNotEmpty() }
    if (duplShortcuts.isNotEmpty()) {
        duplShortcuts.forEach { it ->
            val comp =
                    components.find { comp -> it == (comp as ActionViewWrapper).getShortcut() && comp.isModified() }
            if (comp != null) {
                (comp as ActionViewWrapper).setWarning("Shortcut $it used multiple times")
            }
        }
    }
}

fun setWarningForDuplicateActionNames(
    settings: GlobalSettings.SettingsState,
    components: Array<Component>,
    actionTypes: Array<ActionType>) {
    val allNames = getActionNamesForOtherActionTypes(settings, actionTypes) + getActionNames(components)
    val duplNames = allNames.findDuplicateElements().filter { it -> it.isNotEmpty() }
    if (duplNames.isNotEmpty()) {
        duplNames.forEach { it ->
            val comp =
                    components.find { comp -> it == (comp as ActionViewWrapper).getActionName() && comp.isModified() }
            if (comp != null) {
                (comp as ActionViewWrapper).setWarning("Name $it used multiple times")
            }
        }
    }
}

