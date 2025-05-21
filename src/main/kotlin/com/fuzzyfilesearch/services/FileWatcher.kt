package com.fuzzyfilesearch.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileMoveEvent
import com.intellij.openapi.vfs.VirtualFileCopyEvent
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.util.concurrency.AppExecutorUtil
import com.jetbrains.rd.util.string.println

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

//@Service(Service.Level.PROJECT)
//class FileWatcherService(val project: Project)  {
//    init {
//        val fileIndex = ProjectRootManager.getInstance(project)?.getFileIndex()
//        val connection = ApplicationManager.getApplication().messageBus.connect()
//        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
//            override fun after(events: List<out VFileEvent>) {
//                super.after(events)
//
//                for (event in events) {
//                    event.
//                    println("Event triggered! ${event}")
//
//                }
//                fileIndex?.isInProjectOrExcluded()
//            }
//
//        })
//    }
//}

@Service(Service.Level.PROJECT)
class MyAsyncFileWatcher(project: Project) : Disposable {

    private val mutex = Mutex()
    private var VscTrackedFiles   : MutableSet<VirtualFile> = mutableSetOf()
    private var VscUntrackedFiles : MutableSet<VirtualFile> = mutableSetOf()
    private val connection = ApplicationManager.getApplication().messageBus.connect(this)

    init {
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
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

//                if (isInterestedIn) {
//                    AppExecutorUtil.getAppExecutorService().execute {
//                         long-running task here
//                        println("Handled asynchronously")
//                    }
//                }
            }
        })
    }

    override fun dispose() = Unit

//    fun getListOfFiles(vscTrackedFiles: Boolean): MutableSet<VirtualFile>
//    {
//        mutex.withLock {
//            if (vscTrackedFiles) return VscTrackedFiles
//            if (vscTrackedFiles) return VscTrackedFiles
//        }
//        return VscTrackedFiles
//    }
}
