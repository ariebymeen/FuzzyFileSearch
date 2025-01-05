package com.fuzzyfilesearch.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*

enum class PopupSizePolicy{
    FIXED_SIZE,
    SCALE_WITH_IDE,
    SCALE_WITH_SCREEN
}

enum class PathDisplayType {
    FILENAME_RELATIVE_PATH,
    FILENAME_ONLY,
    FILENAME_FULL_PATH,
    FULL_PATH_WITH_FILENAME,
    RELATIVE_PATH_WITH_FILENAME
}

enum class EditorLocation {
    EDITOR_BELOW,
    EDITOR_RIGHT,
}

enum class ModifierKey {
    CTRL,
    ALT
}

@State(
    name = "com.fuzzyfilesearch.settings.GlobalSettings",
    storages = [Storage(StoragePathMacros.NON_ROAMABLE_FILE)]
)
@Service
class GlobalSettings : PersistentStateComponent<GlobalSettings.SettingsState> {
    private var interalState = SettingsState()

    class SettingsState {
        var popupSizePolicy = PopupSizePolicy.SCALE_WITH_IDE
        var searchPopupWidth = 0.4
        var searchPopupHeight = 0.3
        var searchPopupWidthPx = 700
        var searchPopupHeightPx = 500
        var minSizeEditorPx = 200
        var numberOfFilesInSearchView = 99
        var searchCaseSensitivity = false
        var verticalPositionOnScreen = 0.5
        var horizontalPositionOnScreen = 0.5
        var shrinkViewDynamically = false
        var searchBarHeight = 40
        var searchItemHeight = 30
        var searchOnlyFilesInVersionControl = true
        var excludedDirs: Set<String> = setOf("build", ".gradle", ".idea", ".run")
        var filePathDisplayType: PathDisplayType = PathDisplayType.FILENAME_RELATIVE_PATH
        var modifierKey: ModifierKey = ModifierKey.CTRL
        var openRelativeFileActions: Array<Array<String>> = emptyArray()
        var openRelatedFileAction: Array<Array<String>> = emptyArray() // TODO
        var searchRelativeFileActions: Array<Array<String>> = emptyArray()
        var searchPathActions: Array<Array<String>> = emptyArray()
        var searchFilesMatchingPatterActions: Array<Array<String>> = emptyArray()
        var searchRecentFilesActions: Array<Array<String>> = arrayOf(arrayOf("SearchRecentFiles", "10", "", "alt shift R"))
        var searchOpenFilesActions: Array<Array<String>> = arrayOf(arrayOf("SearchOpenFiles", "", "alt shift O"))
        var showEditorPreview = true
        var editorPreviewLocation = EditorLocation.EDITOR_RIGHT
        var editorSizeRatio = 0.5
        var openWithSingleClick = true
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