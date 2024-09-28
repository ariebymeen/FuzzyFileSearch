package com.quickfilesearch.actions

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.quickfilesearch.searchbox.*
import com.quickfilesearch.services.createHashFileName
import com.quickfilesearch.services.getHashFilePath
import com.quickfilesearch.services.writeLineToFile
import com.quickfilesearch.settings.GlobalSettings
import com.quickfilesearch.settings.PathDisplayType
import com.quickfilesearch.showErrorNotification
import kotlin.math.min

class SearchForFiles(val settings: GlobalSettings.SettingsState) {

    var mFileNamesConcat: String? = null
    var mFileNames: List<String>? = null
    var mPopup: SearchPopupInstance? = null
    var mHasHashFile: Boolean = false
    var mHashFile: String? = null
    var mFiles = emptyList<PopupInstanceItem>()
    var mProject: Project? = null

    fun doSearchForFiles(files: List<PopupInstanceItem>,
                         project: Project,
                         directory: String?,
                         extensions: List<String>?) {
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

        if (directory != null) {
            writeFilesListToHashFiles(project, directory, extensions)
        } else {
            mFileNamesConcat = mFileNames!!.joinToString("\n")
        }

        if (mPopup == null) {
            mPopup = SearchPopupInstance(::getSortedFileList, ::openSelectedFile, settings,  project, extensions)
        } else {
            mPopup?.updatePopupInstance(project, extensions)
        }

        mPopup!!.showPopupInstance()
    }

    private fun writeFilesListToHashFiles(project: Project, directory: String, extensions: List<String>?) {
        val start = System.currentTimeMillis()
//        val mCoroutineScope = CoroutineScope(Dispatchers.Main)
//        hashFile = mProject.service<FileChangeListener>().getHashFilePath(directory, extensions)
        mHashFile = getHashFilePath(project, directory, extensions)
        println("file: $mHashFile")
//        if (!mProject.service<FileChangeListener>().hasValidHash(directory, extensions)) {
            mFileNamesConcat = mFileNames?.joinToString("\n")
            writeLineToFile(mHashFile!!, mFileNamesConcat!!)
//        }
        mHasHashFile = true
        val stop = System.currentTimeMillis()
        println("Time to write hash file: ${stop - start}")
    }

    fun getSortedFileList(query: String) : List<PopupInstanceItem> {
        val filteredmFiles: List<String>
        if (query.isNotEmpty()) {
            filteredmFiles = if (settings.useFzfForSearching) {
                if (mHasHashFile) {
                    runFzfCat(mHashFile!!, query)
                } else {
                    runFzf(mFileNamesConcat!!, query, settings.numberOfFilesInSearchView)
                }
            } else {
                sortCandidatesBasedOnPattern(query, mFileNames!!)
            }

            val visibleList = filteredmFiles.subList(0, min(filteredmFiles.size, settings.numberOfFilesInSearchView))
            val visiblemFiles = visibleList.map { file -> mFileNames!!.indexOfFirst{ name  -> name == file } }
                .map { index ->
                    if (index >= 0) {
                        mFiles[index]
                    } else {
                        println("Error, unexpected index $index. Filtered files size: ${filteredmFiles.size}, file size: ${mFiles.size}")
                        showErrorNotification("Something went wrong searching", "Error searching mFiles: $visibleList, invalidating caches now")
//                        restoreHashes()
                        mFiles[0]
                    }
                }

            return visiblemFiles
        }
        else {
            return mFiles.subList(0, min(mFiles.size, settings.numberOfFilesInSearchView))
        }
    }

//    private fun restoreHashes() {
//        mProject.service<FileChangeListener>().invalidateHashes()
//        writemFilesListToHashmFiles(extensions)
//    }

    fun openSelectedFile(selectedValue: PopupInstanceItem) {
        FileEditorManager.getInstance(mProject!!).openFile(selectedValue.vf, true)
    }

}