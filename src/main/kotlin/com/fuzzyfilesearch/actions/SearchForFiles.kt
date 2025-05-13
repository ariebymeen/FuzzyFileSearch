package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.renderers.FileInstanceItem
import com.fuzzyfilesearch.renderers.FilePathCellRenderer
import com.fuzzyfilesearch.searchbox.*
import com.intellij.openapi.project.Project
import com.fuzzyfilesearch.settings.GlobalSettings
import com.fuzzyfilesearch.settings.PathDisplayType
import com.fuzzyfilesearch.showErrorNotification
import kotlin.math.min

class SearchForFiles(val settings: GlobalSettings.SettingsState) {

    var mFileNames: List<String>? = null
    var mPopup: SearchPopupInstance<FileInstanceItem>? = null
    var mFiles = emptyList<FileInstanceItem>()
    var mProject: Project? = null
    var fzfSearchAction: FzfSearchAction? = null

    fun doSearchForFiles(files: List<FileInstanceItem>,
                         project: Project,
                         directory: String?,
                         extensions: List<String>?) {
        if (files.size > 100000) {
            showErrorNotification("Too many files", "Found ${files.size} files fir searching, " +
                    "please limit the number of files by searching for files" +
                    "with a extension, change the position you search or exclude directories from your search")
            return
        }

        mFiles = files
        mProject = project
        if (settings.filePathDisplayType != PathDisplayType.FILENAME_ONLY) {
            mFileNames = mFiles.map { file ->
                if (isFileInProject(project, file.vf)) {
                    file.vf.path.substring(project.basePath!!.length)
                } else {
                    file.vf.path
                }
            }
        } else {
            mFileNames = mFiles.map { file -> file.vf.name }
        }

        mPopup = SearchPopupInstance(FilePathCellRenderer(project, settings), ::getSortedFileList, ::openFile,
                                                                    ::getFileFromItem, settings,  project, extensions,
                                                                            settings.file,
                                                                            "File search")
        mPopup!!.showPopupInstance()
        fzfSearchAction = FzfSearchAction(mFileNames!!, settings.common.searchCaseSensitivity)
    }

    fun getSortedFileList(query: String) : List<FileInstanceItem> {
        if (query.isNotEmpty()) {
            val filteredFiles = fzfSearchAction!!.search(query)
            val visibleList = filteredFiles.subList(0, min(filteredFiles.size, settings.file.numberOfFilesInSearchView))
            val visibleFiles = visibleList
                .map { file -> mFileNames!!.indexOfFirst{ name  -> name == file } }
                .map { index ->
                    if (index >= 0) {
                        mFiles[index]
                    } else {
                        println("Error, unexpected index $index. Filtered files size: ${filteredFiles.size}, file size: ${mFiles.size}")
                        showErrorNotification("Something went wrong searching", "Error searching mFiles: $visibleList, invalidating caches now")
                        mFiles[0]
                    }
                }

            return visibleFiles
        }
        else {
            return mFiles.subList(0, min(mFiles.size, settings.file.numberOfFilesInSearchView))
        }
    }

    fun openFile(item: FileInstanceItem, location: OpenLocation) {
        openFileWithLocation(item.vf, location, mProject!!)
    }

    fun getFileFromItem(item: FileInstanceItem): FileLocation? {
        return FileLocation(item.vf, 0)
    }
}