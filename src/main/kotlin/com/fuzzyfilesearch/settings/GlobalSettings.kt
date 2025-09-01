package com.fuzzyfilesearch.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*

enum class PopupSizePolicy {
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

enum class ShowFilenamePolicy {
    WHEN_SEARCHING_MULTIPLE_FILES,
    ALWAYS,
    NEVER,
}

enum class ShowSearchDirectoryPolicy {
    SHOW_LAST_DIRECTORY_ONLY,
    SHOW_FROM_PROJECT_ROOT,
    SHOW_FULL_PATH,
    SHOW_NONE,
}

@State(
    name = "com.fuzzyfilesearch.settings.GlobalSettings",
    storages = [Storage(StoragePathMacros.NON_ROAMABLE_FILE)]
      )
@Service
class GlobalSettings : PersistentStateComponent<GlobalSettings.SettingsState> {
    private var internalState = SettingsState()

    class CommonSettings {
        var searchCaseSensitivity = false
        // TODO: REMOVE, this is legacy
        var searchOnlyFilesTrackedByVersionControl = true // TODO: Depreciated
        var excludedDirs: Set<String> = setOf("build", ".gradle", ".idea", ".run")
        var modifierKey: ModifierKey = ModifierKey.CTRL
        var openInVerticalSplit: String = "ctrl S"
        var openInHorizontalSplit: String = "ctrl H"
        var openInActiveEditor: String = "ctrl U"
        var openWithSingleClick = true
        var useDefaultFont = true
        var selectedFontName = "JetBrains Mono"
        var fontSize = 0
        var useDefaultHighlightColor = true
        var selectedColor = ""
        var showTitleInSearchView = true
        var showScrollbar = false
        var titleFontSize = 10 // Font size of the title (showing the action)
        var enableDebugOptions = true
    }

    class PopupSettings {
        var popupSizePolicy = PopupSizePolicy.SCALE_WITH_IDE
        var searchPopupWidth = 0.4
        var searchPopupHeight = 0.3
        var searchPopupWidthPx = 700
        var searchPopupHeightPx = 500
        var minSizeEditorPx = 200
        var numberOfFilesInSearchView = 20
        var showNumberInSearchView = false
        var showFileIcon = true
        var verticalPositionOnScreen = 0.5
        var horizontalPositionOnScreen = 0.5
        var shrinkViewDynamically = false
        var searchBarHeight = 40
        var searchItemHeight = 30
        var showEditorPreview = true
        var editorPreviewLocation = EditorLocation.EDITOR_BELOW
        var editorSizeRatio = 0.5
        var searchMultiThreaded = false
    }

    class SettingsState {
        var common = CommonSettings()
        var file = PopupSettings()
        var string = PopupSettings()

        // All custom actions that can be configured and stored
        var allActions: Array<Array<String>> = emptyArray()

        // TODO: REMOVE, this is legacy, used for porting old settings into the new structure only!
        var openRelativeFileActions: Array<Array<String>> = emptyArray()
        var searchRelativeFileActions: Array<Array<String>> = emptyArray()
        var searchPathActions: Array<Array<String>> = emptyArray()
        var searchFilesMatchingPatterActions: Array<Array<String>> = emptyArray()
        var searchRecentFilesActions: Array<Array<String>> = emptyArray()
        var searchOpenFilesActions: Array<Array<String>> = emptyArray()
        var searchAllFilesActions: Array<Array<String>> = emptyArray()
        // TODO: REMOVE END

        // File search settings
        var filePathDisplayType: PathDisplayType = PathDisplayType.FILENAME_RELATIVE_PATH
        var searchFileNameOnly: Boolean = false
        var searchFileNameMultiplier: Double = 1.0
        var showSearchDirectoryPolicy: ShowSearchDirectoryPolicy = ShowSearchDirectoryPolicy.SHOW_LAST_DIRECTORY_ONLY
        var showSearchDirectoryCutoffLen: Int = 20

        // String search settings
        var applySyntaxHighlightingOnTextSearch = true
        var showFilenameForGrepInFiles = true
        var showFilenameForRegexMatch = ShowFilenamePolicy.WHEN_SEARCHING_MULTIPLE_FILES
        var showLineNumberWithFileName = true
        var useSelectedTextForGrepInFiles = true
        var grepRememberPreviousQuerySeconds: Int = 6
        var minNofLinesBetweenGrepResults: Int = 2
        // TODO: REMOVE, this is legacy, used for porting old settings into the new structure only!
        var searchStringMatchingPatternActions: Array<Array<String>> = emptyArray()
        var searchStringMatchingSubstringActions: Array<Array<String>> = emptyArray()
        // TODO: REMOVE END
    }

    fun getInstance(): GlobalSettings {
        return ApplicationManager.getApplication()
            .getService(GlobalSettings::class.java)
    }

    override fun getState(): SettingsState {
        return internalState
    }

    override fun loadState(storedState: SettingsState) {
        internalState = storedState
    }
}