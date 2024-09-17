package com.quickfilesearch.actions

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.quickfilesearch.searchbox.*
import com.quickfilesearch.services.FileChangeListener
import com.quickfilesearch.services.RecentFilesKeeper
import com.quickfilesearch.services.writeLineToFile
import com.quickfilesearch.settings.GlobalSettings
import com.quickfilesearch.settings.PathDisplayType
import com.quickfilesearch.showErrorNotification
import kotlin.math.min
import kotlinx.coroutines.*
import org.apache.tools.ant.taskdefs.Execute.launch
import org.jetbrains.concurrency.asDeferred
import org.jetbrains.concurrency.asPromise
import org.jetbrains.concurrency.await

class SearchForFiles(val files: List<VirtualFile>,
                     val settings: GlobalSettings.SettingsState,
                     var project: Project,
                     val directory: String? = null,
                     extensions: List<String>? = null) {
    var fileNamesConcat: String? = null
    var fileNames: List<String>? = null
    var popup: PopupInstance? = null
    var hasHashFile: Boolean = false
    var hashFile: String? = null
    val coroutineScope = CoroutineScope(Dispatchers.Main)

    init {
        coroutineScope.launch {
            val processFiles = async {

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

                if (directory != null) {
                    hashFile = project.service<FileChangeListener>().getHashFilePath(directory, extensions)
                    println("Path: ${directory}, resulting hash: $hashFile")
                    if (!project.service<FileChangeListener>().hasValidHash(directory, extensions)) {
                        val start = System.currentTimeMillis()
                        println("No valid hash file present, writing hash file!")
                        fileNamesConcat = fileNames!!.joinToString("\n")
                        writeLineToFile(hashFile!!, fileNamesConcat!!)
                        val stop = System.currentTimeMillis()
                        println("Writing to has file took ${stop - start} ms")
                    }
                    hasHashFile = true
                } else {
                    // TODO: This will never happen
                    fileNamesConcat = fileNames!!.joinToString("\n")
                }
            }

            popup = createPopupInstance(::getSortedFileList, ::openSelectedFile, settings, project.basePath!!, project, extensions)
            processFiles.await()
            updateListedItems(popup!!)
        }

    }

    fun getSortedFileList(query: String) : List<VirtualFile> {
        val filteredFiles: List<String>
        if (query.isNotEmpty()) {
            filteredFiles = if (settings.useFzfForSearching) {
                if (hasHashFile) {
                    runFzfCat(hashFile!!, query)
                } else {
                    // TODO: Remove this method of searching
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