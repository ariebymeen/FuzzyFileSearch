package com.fuzzyfilesearch.services

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.fuzzyfilesearch.searchbox.sha256
import java.io.File

fun createDirectory(path: String) : Boolean {
    val directory = File(path)
    return if (!directory.exists()) {
        directory.mkdirs()
    } else {
        false
    }
}

fun clearDirectoryContents(path: String) {
    val directory = File(path)
    if (directory.exists()) {
        directory.deleteRecursively() // TODO: Maybe a bit wasteful to remove all items
        createDirectory(path)
    }
}

fun directoryContainsFile(path: String, fileName: String): Boolean {
    val directory = File(path)
    if (!directory.exists()) {
        createDirectory(path)
    }

    if (directory.exists() && directory.isDirectory) {
        val files = directory.listFiles() ?: return false
        return files.any { it.name == fileName }
    }
    return false
}


