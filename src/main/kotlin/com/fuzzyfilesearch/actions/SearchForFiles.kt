package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.renderers.FileInstanceItem
import com.fuzzyfilesearch.renderers.FilePathCellRenderer
import com.fuzzyfilesearch.searchbox.*
import com.fuzzyfilesearch.settings.GlobalSettings
import com.fuzzyfilesearch.settings.PathDisplayType
import com.fuzzyfilesearch.showErrorNotification
import com.intellij.openapi.project.Project
import kotlin.math.min
import kotlin.system.measureTimeMillis

class SearchForFiles(val settings: GlobalSettings.SettingsState) {

    var mFilePaths: List<String>? = null
    var mFileNames: List<String>? = null
    var mPopup: SearchPopupInstance<FileInstanceItem>? = null
    var mFiles = emptyList<FileInstanceItem>()
    var mProject: Project? = null
    var fzfSearchAction: FzfSearchAction? = null

    fun search(
        files: List<FileInstanceItem>,
        project: Project,
        extensions: List<String>?,
        searchDirectory: String,
        title: String = "File search") {
        if (files.size > 100000) {
            showErrorNotification(
                "Too many files", "Found ${files.size} files fir searching, " +
                                  "please limit the number of files by searching for files" +
                                  "with a extension, change the position you search or exclude directories from your search")
            return
        }

        mFiles = files
        mProject = project

        mFileNames = mFiles.map { file -> file.vf.name }
        mFilePaths = mFiles.map { file ->
            if (isFileInProject(project, file.vf)) {
                file.vf.path.substring(project.basePath!!.length)
            } else {
                file.vf.path
            }
        }

        mPopup = SearchPopupInstance(
            FilePathCellRenderer(project, settings, searchDirectory), ::getSortedFileList, ::openFile,
            ::getFileFromItem, settings, project, extensions,
            settings.file,
            title,
            utils.getVisualSearchDir(searchDirectory, settings, project.basePath.toString()))
        mPopup!!.showPopupInstance()
        fzfSearchAction = FzfSearchAction(mFilePaths!!,
                                          mFileNames!!,
                                          settings.common.searchCaseSensitivity,
                                          settings.file.searchMultiThreaded,
                                          settings.searchFileNameOnly,
                                          settings.searchFileNameMultiplier)
    }

    fun getSortedFileList(query: String): List<FileInstanceItem> {
        if (query.isNotEmpty()) {
            val visibleFiles: List<FileInstanceItem>
            val timeTaken = measureTimeMillis {
                val (filteredPaths, _) = fzfSearchAction!!.search(query)
                val visibleList = filteredPaths.subList(0, min(filteredPaths.size, settings.file.numberOfFilesInSearchView))
                visibleFiles = visibleList
                    .map { file -> mFilePaths!!.indexOfFirst { name -> name == file } }
                    .map { index ->
                        if (index >= 0) {
                            mFiles[index]
                        } else {
                            println("Error, unexpected index $index. Filtered files size: ${filteredPaths.size}, file size: ${mFiles.size}")
                            showErrorNotification(
                                "Something went wrong searching",
                                "Error searching mFiles: $visibleList, invalidating caches now")
                            mFiles[0]
                        }
                    }
                // We do this here to prevent doing this on the UI thread even though this is horrible and ugly
                visibleFiles.forEach {
                    if (settings.file.showFileIcon && it.icon == null) it.icon = it.vf.fileType.icon
                }
            }
            if (settings.common.enableDebugOptions) {
                println("Searching and sorting files took ${timeTaken} ms")
            }
            return visibleFiles
        } else {
            val visibleFiles = mFiles.subList(0, min(mFiles.size, settings.file.numberOfFilesInSearchView))
            // We do this here to prevent doing this on the UI thread even though this is horrible and ugly
            visibleFiles.forEach {
                if (settings.file.showFileIcon && it.icon == null) it.icon = it.vf.fileType.icon
            }
            return visibleFiles
        }
    }

    fun openFile(item: FileInstanceItem, location: OpenLocation) {
        openFileWithLocation(item.vf, location, mProject!!)
    }

    fun getFileFromItem(item: FileInstanceItem): FileLocation? {
        return FileLocation(item.vf, 0)
    }
}