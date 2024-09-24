package com.quickfilesearch.services

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
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

fun writeLineToFile(filePath: String, line: String) {
    val file = File(filePath)
    file.writeText(line)
}

fun writeLinesToFile(filePath: String, lines: List<String>) {
    val file = File(filePath)
    file.writeText(lines.joinToString(separator = "\n"))
}

fun isFileModified(virtualFile: VirtualFile): Boolean {
    val document = FileDocumentManager.getInstance().getDocument(virtualFile)
    return document != null && FileDocumentManager.getInstance().isDocumentUnsaved(document)
}
