package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.renderers.FileInstanceItem
import com.fuzzyfilesearch.renderers.FilePathCellRenderer
import com.fuzzyfilesearch.searchbox.*
import com.fuzzyfilesearch.settings.GlobalSettings
import com.fuzzyfilesearch.settings.PathDisplayType
import com.fuzzyfilesearch.showErrorNotification
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import kotlin.math.min
import kotlin.system.measureTimeMillis

class SearchForFiles(val settings: GlobalSettings.SettingsState) {

    var mFilePaths: List<String>? = null
    var mFileNames: List<String>? = null
    var mPopup: SearchPopupInstance<FileInstanceItem>? = null
    var mFiles = emptyList<FileInstanceItem>()
    var mProject: Project? = null
    var fzfSearchAction: FzfSearchAction? = null
    // Allow restoring search query by pressing array up before typing
    var mPreviousSearchQuery: String = ""

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

        // All callers already pass unique files (the VFS walk cannot produce
        // duplicates), so avoid the extra full-list hash pass on popup open.
        mFiles = files
        mProject = project

        // Build the name and display-path lists in one pass. The old code
        // called isFileInProject() (a file-index lookup) once per file before
        // the popup was shown, which made opening large searches slow. For
        // display purposes a project-base-path prefix check is equivalent for
        // files inside the project and avoids the per-file index access.
        val basePath = project.basePath
        val names = ArrayList<String>(mFiles.size)
        val paths = ArrayList<String>(mFiles.size)
        for (file in mFiles) {
            val path = file.vf.path
            names.add(file.vf.name)
            val inProject = basePath != null && path.startsWith(basePath) &&
                    (path.length == basePath.length || path[basePath.length] == '/')
            paths.add(if (inProject) path.substring(basePath!!.length) else path)
        }
        mFileNames = names
        mFilePaths = paths

        // Create the search index before showing the popup: the popup starts
        // fetching results on a pooled thread immediately, and a restored
        // non-empty query must not race the fzfSearchAction assignment.
        fzfSearchAction = FzfSearchAction(mFilePaths!!,
                                          mFileNames!!,
                                          settings.common.searchCaseSensitivity,
                                          settings.file.searchMultiThreaded,
                                          settings.searchFileNameOnly,
                                          settings.searchFileNameMultiplier)

        mPopup = SearchPopupInstance(
            FilePathCellRenderer(project, settings, searchDirectory), ::getSortedFileList, ::openFile,
            ::getFileFromItem, settings, project, extensions,
            settings.file,
            title,
            utils.getVisualSearchDir(searchDirectory, settings, project.basePath.toString()),
            mPreviousSearchQuery)
        mPopup!!.showPopupInstance("", min(mFiles.size, settings.file.numberOfFilesInSearchView))
    }

    fun getSortedFileList(query: String): List<FileInstanceItem> {
        if (query.isNotEmpty()) {
            val visibleFiles: List<FileInstanceItem>
            val timeTaken = measureTimeMillis {
                fzfSearchAction!!.search(query)
                // The sort just published previousResultPaths (and the
                // matching index list). Map the first K entries back to
                // mFiles in O(K) using the index list, instead of the
                // previous O(K * M) indexOfFirst scan.
                val sortedIndices = fzfSearchAction!!.resultIndices()
                visibleFiles = mapVisibleItems(
                    sortedIndices,
                    settings.file.numberOfFilesInSearchView,
                    mFiles,
                    mFiles[0])
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
        mPreviousSearchQuery = mPopup?.getQuery() ?: ""
        if (!item.vf.isValid) { // sometimes the files are refreshed causing these to not be valid
            val vf = LocalFileSystem.getInstance().findFileByPath(item.vf.path)
            if (vf != null) {
                openFileWithLocation(vf, location, mProject!!)
            }
        }
        openFileWithLocation(item.vf, location, mProject!!)
    }

    fun getFileFromItem(item: FileInstanceItem): FileLocation {
        return FileLocation(item.vf, 0)
    }
}