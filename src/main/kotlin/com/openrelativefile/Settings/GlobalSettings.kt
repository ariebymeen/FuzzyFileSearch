package com.openrelativefile.Settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*

enum class PathDisplayType {
    FILENAME_ONLY,
    FILENAME_RELATIVE_PATH,
    FILENAME_FULL_PATH,
    FULL_PATH_WITH_FILENAME
}

@State(
    name = "com.openrelativefile.settings.GlobalSettings",
    storages = [Storage(StoragePathMacros.NON_ROAMABLE_FILE)]
)
@Service
class GlobalSettings : PersistentStateComponent<GlobalSettings.SettingsState> {
    private var interalState = SettingsState()

    class SettingsState {
        var searchPopupWidth = 0.4;
        var searchPopupHeight = 0.3;
        var numberOfFilesInSearchView = 10;
        var useFzfForSearching = true
        var excludedDirs: Set<String> = setOf("build", ".gradle", ".idea", ".run")
        var filePathDisplayType: PathDisplayType = PathDisplayType.FILENAME_ONLY
        var openRelativeFileActions: Array<Array<String>> = emptyArray()
        var searchRelativeFileActions: Array<Array<String>> = emptyArray()
        var searchPathActions: Array<Array<String>> = emptyArray()
    }

    fun getInstance() : GlobalSettings {
        return ApplicationManager.getApplication()
            .getService(GlobalSettings::class.java)
    }

    override fun getState(): SettingsState {
        return interalState;
    }

    override fun loadState(storedState: SettingsState) {
        interalState = storedState
    }
}