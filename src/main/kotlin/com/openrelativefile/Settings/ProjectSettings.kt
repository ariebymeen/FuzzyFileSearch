package com.openrelativefile.Settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*


@State(
    name = "com.openrelativefile.settings.ProjectSettings",
    storages = [Storage("OpenRelativeFileSettings.xml")]
)
@Service
class ProjectSettings : PersistentStateComponent<ProjectSettings.SettingsState> {
    private var interalState = SettingsState()

    class SettingsState {
        var openRelativeFileActions: Array<Array<String>> = emptyArray()
        var searchRelativeFileActions: Array<Array<String>> = emptyArray()
        var searchPathActions: Array<Array<String>> = emptyArray()
    }

    fun getInstance() : ProjectSettings {
        return ApplicationManager.getApplication()
            .getService(ProjectSettings::class.java)
    }

    override fun getState(): SettingsState {
        return interalState;
    }

    override fun loadState(storedState: SettingsState) {
        interalState = storedState
    }
}