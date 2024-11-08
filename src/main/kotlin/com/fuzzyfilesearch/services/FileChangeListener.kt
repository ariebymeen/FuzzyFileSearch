package com.fuzzyfilesearch.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileCopyEvent
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileMoveEvent
import com.fuzzyfilesearch.searchbox.sha256

@Service(Service.Level.PROJECT)
class FileChangeListener(private val project: Project) : VirtualFileListener {
    val directory: String

    init {
        if (project.basePath != null) {
            directory = "/tmp/.${project.basePath!!.sha256()}/"
        } else {
            directory = "/tmp/.project/"
        }
        createDirectory(directory)
        clearDirectoryContents(directory)
    }

    fun hasValidHash(path: String, extensions: List<String>? = null): Boolean {
        return directoryContainsFile(directory, createHashFileName(path, extensions))
    }

    fun hasValidHashFile(fileName: String): Boolean {
        return directoryContainsFile(directory, fileName)
    }

    fun getHashFilePath(path: String, extensions: List<String>? = null): String {
        return "$directory/${createHashFileName(path, extensions)}"
    }

    private fun createHashFileName(path: String, extensions: List<String>? = null) : String {
        if (extensions != null) {
            val hash = "$path;${extensions.joinToString(";")}".sha256()
            return ".$hash"
        } else {
            return ".${path.sha256()}"
        }
    }

//    fun invalidateHashes() {
//        clearDirectoryContents(directory)
//    }

    override fun fileCreated(event: VirtualFileEvent) {
        super.fileCreated(event)
        clearDirectoryContents(directory)
        println("File created: ${event.file.path}")
        // TODO: This
    }

    override fun fileDeleted(event: VirtualFileEvent) {
        super.fileDeleted(event)
        clearDirectoryContents(directory)
        println("File deleted: ${event.file.path}")
    }

    override fun fileMoved(event: VirtualFileMoveEvent) {
        super.fileMoved(event)
        clearDirectoryContents(directory)
        println("File moved: ${event.file.path}")
    }

    override fun fileCopied(event: VirtualFileCopyEvent) {
        super.fileCopied(event)
        clearDirectoryContents(directory)
        println("File copied: ${event.file.path}")
    }

    // Called when a file's content is changed
//    override fun contentsChanged(event: VirtualFileEvent) {
//        println("File content changed: ${event.file.path}")
//    }

}