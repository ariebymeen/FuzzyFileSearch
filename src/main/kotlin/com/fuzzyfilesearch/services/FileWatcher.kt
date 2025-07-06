package com.fuzzyfilesearch.services

import com.fuzzyfilesearch.renderers.FileInstanceItem
import com.fuzzyfilesearch.searchbox.getAllFilesInRoot
import com.fuzzyfilesearch.searchbox.isFileInProject
import com.fuzzyfilesearch.settings.GlobalSettings
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
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
import com.intellij.openapi.startup.ProjectActivity

@Service(Service.Level.PROJECT)
class FileWatcher(var mProject: Project) : Disposable {

    private val mVcsTrackedMutex = Mutex()
    private val mVcsUntrackedMutex = Mutex()
    private var mSettings = GlobalSettings.SettingsState()
    private var changeListManager = ChangeListManager.getInstance(mProject)

    // TODO: This list now keeps references of items between search settings, so changing the height of a file
    //       might not work as expected (keeps the old heigth as the JPanel item is already created
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
                val events = events.filter{ event ->
                    when (event) {
                        is VFileDeleteEvent -> isFileInProject(mProject, event.file)
                        is VFileCopyEvent -> isFileInProject(mProject, event.file)
                        is VFileCreateEvent -> isFileInProject(mProject, event.file!!)
                        is VFileMoveEvent -> isFileInProject(mProject, event.file)
                        is VFilePropertyChangeEvent -> {
                            if (event.propertyName == VirtualFile.PROP_NAME && isFileInProject(mProject, event.file)) {
                                true
                            }
                            else false
                        }
                        else -> false
                    }
                }

                if (events.isNotEmpty()) {
                    AppExecutorUtil.getAppExecutorService().execute {
                        runBlocking {
                            val isTracked = events.any{
                                if (it.file == null) {
                                    false
                                } else {
                                    !changeListManager.isIgnoredFile(it.file!!)
                                }
                            }
                            if (isTracked) {
                                mVcsTrackedMutex.withLock {
                                    mVcsTrackedFiles.clear()
                                }
                            }
                            mVcsUntrackedMutex.withLock {
                                mVcsUntrackedFiles.clear()
                            }

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
            mVcsTrackedFiles = getAllFilesInRoot(
                virtualFile,
                mSettings.common.excludedDirs,
                emptyList(),
                ChangeListManager.getInstance(mProject)
            )
        }
    }

    suspend fun refreshVcsUntrackedFiles() {
        val virtualFile = VfsUtil.findFile(Path(mProject.basePath!!), true)
        assert(virtualFile != null)
        virtualFile ?: return
        mVcsUntrackedMutex.withLock {
            mVcsUntrackedFiles = getAllFilesInRoot(virtualFile,
                mSettings.common.excludedDirs,
                emptyList(),
                null)
        }
    }

    fun getListOfFiles(parent: VirtualFile,
                       project: Project, // TODO: This should not be needed! But I found that sometimes the change listener is not working,
                                         // TODO: Seemingly due to the project not being set / loaded
                       onlyVcsTrackedFiles: Boolean,
                       isIncluded: ((VirtualFile) -> Boolean)?): List<FileInstanceItem> {
        var files: List<FileInstanceItem> = emptyList()
        runBlocking {
            launch(Dispatchers.Default) {
                if (onlyVcsTrackedFiles) {
                    mVcsTrackedMutex.withLock {
                        mProject = project
                        changeListManager = ChangeListManager.getInstance(mProject)
                        files = findIncludedChildFiles(parent, mVcsTrackedFiles, isIncluded)
                    }
                }
                else {
                    mVcsUntrackedMutex.withLock {
                        // If no files are loaded yet, reload the files
                        if (mVcsUntrackedFiles.isEmpty()) {
                            AppExecutorUtil.getAppExecutorService().execute {
                                runBlocking {
                                    launch(Dispatchers.Default) {
                                        refreshVcsUntrackedFiles()
                                    }
                                }
                            }
                        }
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
}

class ProjectLoadedOrChangedRefresh : ProjectActivity {
    override suspend fun execute(project: Project) {
        // On project load, refresh files, to ensure always correct state (Vcs could have changed)
        AppExecutorUtil.getAppExecutorService().execute {
            runBlocking {
                launch(Dispatchers.Default) {
                    project.service<FileWatcher>().refreshFileCache()
                }
            }
        }
    }
}