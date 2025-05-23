package com.fuzzyfilesearch.searchbox

import ai.grazie.text.find
import com.fuzzyfilesearch.renderers.FileInstanceItem
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import java.security.MessageDigest
import javax.swing.SwingConstants
import kotlin.collections.ArrayList

fun getParentSatisfyingRegex(project: Project,
                             directory: VirtualFile,
                             regex: Regex,
                             excludedDirs: Set<String>? = null,
                             distance: Int = 0,
                             maxDistance: Int = 1000): VirtualFile? {
    for (child in directory.children!!) {
        if (child.isFile) {
            if (regex.matches(child.name) || regex.pattern == child.name) return child
        }
    }

    directory.parent ?: return null

    if (distance <= maxDistance && isFileInProject(project, directory.parent)) {
        val file = getParentSatisfyingRegex(project, directory.parent, regex, excludedDirs, distance + 1, maxDistance);
        if (file != null) return file
    }

    return null
}

//fun getFileSatisfyingRegex(project: Project,
//                           directory: VirtualFile,
//                           regex: Regex,
//                           excludedDirs: Set<String>? = null,
//                           excludeDirectory: VirtualFile? = null,
//                           distance: Int = 0,
//                           maxDistance: Int = 1000): VirtualFile? {
//    for (child in directory.children!!) {
//        if (child.isFile) {
//            if (regex.matches(child.name) || regex.pattern == child.name) return child
//        } else {
//            val isExcludedDir = (excludedDirs != null && excludedDirs.contains(child.name))
//
//            if (child.isDirectory && !isExcludedDir && distance <= maxDistance) {
//                val file = getFileSatisfyingRegex(project, child, regex, excludedDirs, directory, distance + 1, maxDistance);
//                if (file != null) return file
//            }
//        }
//    }
//    directory.parent ?: return null
//
//    val isPreviouslyVisited = (excludeDirectory != null && excludeDirectory == directory.parent)
//    if (distance <= maxDistance && !isPreviouslyVisited && isFileInProject(project, directory.parent)) {
//        val file = getFileSatisfyingRegex(project, directory.parent, regex, excludedDirs, directory, distance + 1, maxDistance);
//        if (file != null) return file
//    }
//
//    return null
//}

fun isFileInProject(project: Project, file: VirtualFile): Boolean {
    val projectRootManager = ProjectRootManager.getInstance(project)
    if (!project.isInitialized) {
        return file.path.find(project.basePath!!) != null
    }

    // If file is in current project or otherwise check if the file is loaded in the file tree
    return projectRootManager.fileIndex.isInContent(file)
            || (projectRootManager.contentRoots.isEmpty()
                        && file.path.length >= project.basePath!!.length
                        && file.path.subSequence(0, project.basePath!!.length) == project.basePath!!)
}

//fun getRootAtMaxDistance(referenceFile: VirtualFile, maxDistance: Int, project: Project) : VirtualFile {
//    var distance = 1
//    var maxDistanceRoot = referenceFile.parent
//    while (distance < maxDistance) {
//        if (isFileInProject(project, maxDistanceRoot.parent!!)) {
//            ++distance
//            maxDistanceRoot = maxDistanceRoot.parent!!
//        } else {
//            return maxDistanceRoot // file is no longer in project, stop searching
//        }
//    }
//    return maxDistanceRoot;
//}
//fun computePathDistance(file1: VirtualFile, file2: VirtualFile): Int {
//    val file1Dirs = file1.path.split('/')
//    val file2Dirs = file2.path.split('/')
//    val maxSize = min(file1Dirs.size, file2Dirs.size)
//
//    var commonCtr = 0
//    while (commonCtr + 1 < maxSize && file1Dirs[commonCtr + 1] == file2Dirs[commonCtr + 1]) {
//        ++commonCtr
//    }
//
//    return file1Dirs.size + file2Dirs.size - 2 * commonCtr - 2
//}

//fun getAllFilesInRootWithinDistance(root: VirtualFile, referenceFile: VirtualFile,
//                                    maxDistance: Int,
//                                    excludedDirectoryList: Set<String>? = null) : ArrayList<VirtualFile> {
//    var files: ArrayList<VirtualFile> = ArrayList()
//    if (!root.isDirectory) files.add(root)
//
//    for (child in root.children!!) {
//        val distance = computePathDistance(child, referenceFile)
//
//        if (child.isFile && distance <= maxDistance) {
//            files.add(child)
//        }
//        if (child.isDirectory && distance <= maxDistance) {
//            if (excludedDirectoryList != null && excludedDirectoryList.contains(child.name)) continue
//            files.addAll(getAllFilesInRootWithinDistance(child, referenceFile, maxDistance, excludedDirectoryList))
//        }
//    }
//    return files
//}
//
//fun getAllFilesWithinDistance(referenceFile: VirtualFile,
//                              maxDistance: Int,
//                              project: Project,
//                              excludedDirectoryList: Set<String>? = null) : ArrayList<VirtualFile> {
//    val maxRoot = getRootAtMaxDistance(referenceFile, maxDistance, project)
//    return getAllFilesInRootWithinDistance(maxRoot, referenceFile, maxDistance, excludedDirectoryList)
//}

fun getAllFilesInRoot(root: VirtualFile,
                      excludedDirectoryList: Set<String>? = null,
                      extensions: List<String>,
                      vcsManager: ChangeListManager? = null) : ArrayList<FileInstanceItem> {
    val files: ArrayList<FileInstanceItem> = ArrayList()
    if (!root.isDirectory) files.add(FileInstanceItem(root))

    for (child in root.children!!) {
        if (vcsManager != null && vcsManager.isIgnoredFile(child)) continue

        if (child.isFile && (extensions.isEmpty() || extensions.contains(child.extension))) {
            files.add(FileInstanceItem(child))
        }
        if (child.isDirectory) {
            if (excludedDirectoryList != null && excludedDirectoryList.contains(child.name)) continue
            files.addAll(getAllFilesInRoot(child, excludedDirectoryList, extensions, vcsManager))
        }
    }
    return files
}

fun String.sha256(): String {
    val md = MessageDigest.getInstance("SHA-256")
    return md.digest(toByteArray()).joinToString("") { "%02x".format(it) }
}

fun openFileWithLocation(vf: VirtualFile, location: OpenLocation, project: Project) {
    val manager = FileEditorManager.getInstance(project)
    when (location) {
        OpenLocation.MAIN_VIEW -> manager.openFile(vf, true)
        OpenLocation.SPLIT_VIEW_VERTICAL -> {
            FileEditorManagerEx.getInstanceEx(project).currentWindow?.split(
                SwingConstants.VERTICAL,
                true,
                vf,
                true
            )
        }

        OpenLocation.SPLIT_VIEW_HORIZONTAL -> {
            FileEditorManagerEx.getInstanceEx(project).currentWindow?.split(
                SwingConstants.HORIZONTAL,
                true,
                vf,
                true
            )
        }
    }
}
