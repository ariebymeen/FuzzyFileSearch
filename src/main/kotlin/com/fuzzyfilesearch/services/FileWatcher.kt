package com.fuzzyfilesearch.services

import com.fuzzyfilesearch.renderers.FileInstanceItem
import com.fuzzyfilesearch.searchbox.getAllFilesInRoot
import com.fuzzyfilesearch.settings.GlobalSettings
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.util.concurrency.AppExecutorUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.io.path.Path

@Service(Service.Level.PROJECT)
class FileWatcher(val mProject: Project) : Disposable {

    private val mVcsTrackedMutex = Mutex()
    private val mVcsUntrackedMutex = Mutex()
    private var mSettings = GlobalSettings.SettingsState()
    private var mVcsTrackedFiles   : ArrayList<FileInstanceItem> = ArrayList<FileInstanceItem>()
    private var mVcsUntrackedFiles : ArrayList<FileInstanceItem> = ArrayList<FileInstanceItem>()
    private val mConnection = ApplicationManager.getApplication().messageBus.connect(this)

    init {
        // Get a list of files at startup
        AppExecutorUtil.getAppExecutorService().execute {
            runBlocking {
                launch(Dispatchers.Default) {
                    refreshFileCache()
                }
            }
        }

        mConnection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                val isInterestedIn = events.any { event ->
                    when (event) {
                        is VFileDeleteEvent -> true
                        is VFileCopyEvent -> true
                        is VFileCreateEvent -> true
                        is VFileMoveEvent -> true
                        is VFilePropertyChangeEvent -> {
                            if (event.propertyName == VirtualFile.PROP_NAME) {
                                true
                            }
                            else false
                        }
                        else -> false
                    }
                }
                println("File changed event: is interested in: ${isInterestedIn}")

                if (isInterestedIn) {
                    AppExecutorUtil.getAppExecutorService().execute {
                        runBlocking {
                            launch(Dispatchers.Default) {
                                refreshFileCache()
                            }
                        }
                    }
                }
            }
        })
    }

    override fun dispose() = Unit

    fun setSettings(settings: GlobalSettings.SettingsState) {
        mSettings = settings
    }

    suspend fun refreshFileCache() {
        val virtualFile = VfsUtil.findFile(Path(mProject.basePath!!), true)
        assert(virtualFile != null)
        virtualFile ?: return // TODO: This should really not be possible!
        mVcsTrackedMutex.withLock {
            println("Refreshing file cache")
            mVcsTrackedFiles = getAllFilesInRoot(
                virtualFile,
                mSettings.common.excludedDirs,
                emptyList(),
                ChangeListManager.getInstance(mProject)
            )
        }
        mVcsUntrackedMutex.withLock {
            mVcsUntrackedFiles = getAllFilesInRoot(virtualFile,
                mSettings.common.excludedDirs,
                emptyList(),
                null)
        }
    }

    fun getListOfFiles(parent: VirtualFile,
                       onlyVcsTrackedFiles: Boolean,
                       isIncluded: ((VirtualFile) -> Boolean)?): List<FileInstanceItem> {
        var files: List<FileInstanceItem> = emptyList()
        runBlocking {
            launch(Dispatchers.Default) {
                if (onlyVcsTrackedFiles) {
                    mVcsTrackedMutex.withLock {
                        files = findIncludedChildFiles(parent, mVcsTrackedFiles, isIncluded)
                    }
                }
                else {
                    mVcsUntrackedMutex.withLock {
                        files = findIncludedChildFiles(parent, mVcsUntrackedFiles, isIncluded)
                    }

                }
            }.join()
        }
        return files
    }

    fun findIncludedChildFiles(parent: VirtualFile, files: ArrayList<FileInstanceItem>,
                               isIncluded: ((VirtualFile) -> Boolean)?): List<FileInstanceItem> {
        val parentPath = parent.path
        val parentPathLength = parentPath.length
        return files.filter {
            it.vf.path.length > parentPathLength &&
                    (isIncluded == null || isIncluded(it.vf) &&
                    it.vf.path.startsWith(parentPath) &&
                    (it.vf.path[parentPathLength] == '/'))
        }
    }

    // This is the most correct method. Choose this if there are edge cases
    //fun findChildFiles(parent: VirtualFile, files: List<VirtualFile>): List<VirtualFile> {
    //    return files.filter { VirtualFileManager.getInstance().isAncestor(parent, it) }
    //}

}
