package com.quickfilesearch.actions

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.quickfilesearch.searchbox.*
import com.quickfilesearch.services.FileChangeListener
import com.quickfilesearch.services.writeLineToFile
import com.quickfilesearch.settings.GlobalSettings
import com.quickfilesearch.settings.PathDisplayType
import com.quickfilesearch.showErrorNotification
import kotlin.math.min
import kotlinx.coroutines.*

class SearchForFiles(val files: List<PopupInstanceItem>,
                     val settings: GlobalSettings.SettingsState,
                     var project: Project,
                     val directory: String? = null,
                     extensions: List<String>? = null) {
    var fileNamesConcat: String? = null
    var fileNames: List<String>? = null
//    var popup: PopupInstance? = null
    var popup = SearchPopupInstance(::getSortedFileList, ::openSelectedFile, settings, project.basePath!!, project, extensions)
    var hasHashFile: Boolean = false
    var hashFile: String? = null
    val coroutineScope = CoroutineScope(Dispatchers.Main)

    init {
//        coroutineScope.launch {
//            val processFiles = async {
        val start = System.currentTimeMillis()
                if (settings.filePathDisplayType != PathDisplayType.FILENAME_ONLY) {
                    fileNames = files.map { file ->
                        if (isFileInProject(project, file.vf)) {
                            file.vf.path.substring(project.basePath!!.length)
                        } else {
                            file.vf.path
                        }
                    }
                } else {
                    fileNames = files.map { file -> file.vf.name }
                }

                if (directory != null) {
                    writeFilesListToHashFiles(extensions)
                } else {
                    fileNamesConcat = fileNames!!.joinToString("\n")
                }
//            }

        val stop = System.currentTimeMillis()
        println("Concatting all filenames and writing hash file look ${stop - start} ms")

        val startPopup = System.currentTimeMillis()
//        popup = createPopupInstance(::getSortedFileList, ::openSelectedFile, settings, project.basePath!!, project, extensions)
        popup.showPopupInstance()
        val stopPopup = System.currentTimeMillis()
        println("Loading popup took ${stopPopup - startPopup} ms")
//            processFiles.await()
//            updateListedItems(popup!!)
        println("End of action: ${System.currentTimeMillis()}")
//        }

    }

    private fun writeFilesListToHashFiles(extensions: List<String>?) {
        val directory = directory ?: return
        hashFile = project.service<FileChangeListener>().getHashFilePath(directory, extensions)
        println("Path: ${directory}, resulting hash: $hashFile")
        if (!project.service<FileChangeListener>().hasValidHash(directory, extensions)) {
            val start = System.currentTimeMillis()
            println("No valid hash file present, writing hash file!")
            fileNamesConcat = fileNames!!.joinToString("\n")
            writeLineToFile(hashFile!!, fileNamesConcat!!)
            val stop = System.currentTimeMillis()
            println("Writing to hash file took ${stop - start} ms")
        }
        hasHashFile = true
    }

    fun getSortedFileList(query: String) : List<PopupInstanceItem> {
        val filteredFiles: List<String>
        if (query.isNotEmpty()) {
            filteredFiles = if (settings.useFzfForSearching) {
                if (hasHashFile) {
                    runFzfCat(hashFile!!, query)
                } else {
                    runFzf(fileNamesConcat!!, query, settings.numberOfFilesInSearchView)
                }
            } else {
                sortCandidatesBasedOnPattern(query, fileNames!!)
            }

            val visibleList = filteredFiles.subList(0, min(filteredFiles.size, settings.numberOfFilesInSearchView))
            val visibleFiles = visibleList.map { file -> fileNames!!.indexOfFirst{ name  -> name == file } }
                .map { index ->
                    if (index >= 0) {
                        files[index]
                    } else {
                        println("Error, unexpected index $index. Filtered files size: ${filteredFiles.size}, file size: ${files.size}")
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

    fun openSelectedFile(selectedValue: PopupInstanceItem) {
        FileEditorManager.getInstance(project).openFile(selectedValue.vf, true)
    }

}