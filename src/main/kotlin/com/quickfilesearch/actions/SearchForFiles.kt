package com.quickfilesearch.actions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.quickfilesearch.searchbox.*
import com.quickfilesearch.settings.GlobalSettings
import com.quickfilesearch.settings.PathDisplayType
import com.quickfilesearch.showErrorNotification
import kotlin.math.min

class SearchForFiles(val files: List<VirtualFile>,
                     val settings: GlobalSettings.SettingsState,
                     var project: Project,
                     extensions: List<String>? = null) {
    var fileNamesConcat: String
    var fileNames: List<String>
    var popup: PopupInstance

    init {
        if (files.size > 500) {
            showErrorNotification("Too many files to search", """
                Too many files ${files.size} found, so these cannot be searched. Consider excluding directory with too 
                many files or specifying what extensions you are interested in """)
        }

        if (settings.filePathDisplayType != PathDisplayType.FILENAME_ONLY) {
            fileNames = files.map { file ->
                if (isFileInProject(project, file)) {
                    file.path.substring(project.basePath!!.length)
                } else {
                    file.path
                }
            }
        } else {
            fileNames = files.map { file -> file.name }
        }
        fileNamesConcat = fileNames.joinToString("\n")
        popup = createPopupInstance(::getSortedFileList, ::openSelectedFile, settings, project.basePath!!, project, extensions)
    }

    fun getSortedFileList(query: String) : List<VirtualFile> {
        val filteredFiles: List<String>
        if (query.isNotEmpty()) {
            if (settings.useFzfForSearching) {
                filteredFiles = runFzf(fileNamesConcat, query, settings.numberOfFilesInSearchView)
            } else {
                filteredFiles = sortCandidatesBasedOnPattern(query, fileNames)
            }

            val visibleList = filteredFiles.subList(0, min(filteredFiles.size, settings.numberOfFilesInSearchView))
            val visibleFiles = visibleList.map { file -> fileNames.indexOfFirst{ name  -> name == file } }
                .map { index ->
                    if (index >= 0) {
                        files[index]
                    } else {
                        println("Error, unexpected index < 0. Filtered files size: ${filteredFiles.size}, file size: ${files.size}, index: $index")
                        showErrorNotification("Something went wrong searching", "$visibleList")
                        files[0]
                    }
                }

            return visibleFiles
        }
        else {
            return files.subList(0, min(files.size, settings.numberOfFilesInSearchView))
        }
    }

    fun openSelectedFile(selectedValue: VirtualFile) {
        FileEditorManager.getInstance(project).openFile(selectedValue, true)
    }

}