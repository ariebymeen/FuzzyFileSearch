package com.quickfilesearch.services

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.quickfilesearch.searchbox.sha256
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

fun getHashFilePath(project: Project, path: String, extensions: List<String>? = null): String {
    val directory = "/tmp/.${project.basePath!!.sha256()}"
    if (!File(directory).exists()) {
        createDirectory(directory)
    }
    return "$directory/${createHashFileName(path, extensions)}"
}

fun createHashFileName(path: String, extensions: List<String>? = null) : String {
    if (extensions != null) {
        val hash = "$path;${extensions.joinToString(";")}".sha256()
        return ".$hash"
    } else {
        return ".${path.sha256()}"
    }
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
