package com.openrelativefile.Actions

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.openrelativefile.PopupInstance
import com.openrelativefile.Settings.GlobalSettings
import com.openrelativefile.Settings.PathDisplayType
import com.openrelativefile.createPopupInstance
import com.openrelativefile.runFzf
import com.openrelativefile.sortCandidatesBasedOnPattern
import kotlin.math.min

class SearchForFiles(val files: List<VirtualFile>,
                     val settings: GlobalSettings.SettingsState,
                     var project: Project) {
    var fileNamesConcat: String
    var fileNames: List<String>
    var popup: PopupInstance


    init {
        if (settings.filePathDisplayType != PathDisplayType.FILENAME_ONLY) {
            fileNames = files.map { file -> file.path }
        } else {
            fileNames = files.map { file -> file.name}
        }
        fileNamesConcat = fileNames.joinToString("\n")
        popup = createPopupInstance(::getSortedFileList, ::openSelectedFile, settings, project.basePath!!)
    }

    fun getSortedFileList(query: String) : List<VirtualFile> {
        val start = System.nanoTime();

        var startFzf = start // TODO: Remove
        var stopFzf = start
        var filteredFiles: List<String>
        if (query.isNotEmpty()) {
            if (settings.useFzfForSearching) {
                startFzf = System.nanoTime()
                filteredFiles = runFzf(fileNamesConcat, query, settings.numberOfFilesInSearchView)
                stopFzf = System.nanoTime()
            } else {
                filteredFiles = sortCandidatesBasedOnPattern(query, files.map { file -> file.name })
            }

            println("Nof files: ${filteredFiles.size}, nofFilesInView: ${settings.numberOfFilesInSearchView}")
            val visibleList = filteredFiles.subList(0, min(filteredFiles.size, settings.numberOfFilesInSearchView))
            val visibleFiles = visibleList.map { file -> fileNames.indexOfFirst{ name  -> name == file } }
                .map { index -> files[index] }

            val stop = System.nanoTime();
            println("Searching through files took ${(stop - start) / 1000000} ms. Fzf took ${(stopFzf - startFzf) /1000000}, nof files: ${files.size}")
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