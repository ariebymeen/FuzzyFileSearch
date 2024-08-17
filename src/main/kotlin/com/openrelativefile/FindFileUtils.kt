package com.openrelativefile

import ai.grazie.text.find
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.min
import com.intellij.openapi.progress.ModalTaskOwner.project


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

fun getFileSatisfyingRegex(project: Project,
                           directory: VirtualFile,
                           regex: Regex,
                           excludedDirs: Set<String>? = null,
                           excludeDirectory: VirtualFile? = null,
                           distance: Int = 0,
                           maxDistance: Int = 1000): VirtualFile? {
    println("Searching in directory ${directory.path}")
    for (child in directory.children!!) {
        if (child.isFile) {
            if (regex.matches(child.name) || regex.pattern == child.name) return child
        } else {
            val isExcludedDir = (excludedDirs != null && excludedDirs.contains(child.name))

            if (child.isDirectory && !isExcludedDir && distance <= maxDistance) {
                val file = getFileSatisfyingRegex(project, child, regex, excludedDirs, directory, distance + 1, maxDistance);
                if (file != null) return file
            }
        }
    }
    directory.parent ?: return null

    val isPreviouslyVisited = (excludeDirectory != null && excludeDirectory == directory.parent)
    println("Distance: $distance, maxDistance: $maxDistance, isPreviouslyVisited: $isPreviouslyVisited, isFileInProj: ${isFileInProject(project, directory.parent)}")
    if (distance <= maxDistance && !isPreviouslyVisited && isFileInProject(project, directory.parent)) {
        val file = getFileSatisfyingRegex(project, directory.parent, regex, excludedDirs, directory, distance + 1, maxDistance);
        if (file != null) return file
    }

    return null
}

fun isFileInProject(project: Project, file: VirtualFile): Boolean {
    val projectRootManager = ProjectRootManager.getInstance(project)
    // If file is in current project or otherwise check if the file is loaded in the file tree
    return projectRootManager.fileIndex.isInContent(file) || (projectRootManager.contentRoots.isEmpty() && file.path.find(project.basePath!!) != null)
}

fun getRootAtMaxDistance(referenceFile: VirtualFile, maxDistance: Int, project: Project) : VirtualFile {
    var distance = 1
    var maxDistanceRoot = referenceFile.parent // TODO: How get parent without suddenly having all files not belonging to the project
    while (distance < maxDistance) {
        if (isFileInProject(project, maxDistanceRoot.parent!!)) {
            ++distance
            maxDistanceRoot = maxDistanceRoot.parent!!
        } else {
            return maxDistanceRoot // file is no longer in project, stop searching
        }
    }
    return maxDistanceRoot;
}
fun computePathDistance(file1: VirtualFile, file2: VirtualFile): Int {
    val file1Dirs = file1.path.split('/')
    val file2Dirs = file2.path.split('/')
    val maxSize = min(file1Dirs.size, file2Dirs.size)

    var commonCtr = 0
    while (commonCtr + 1 < maxSize && file1Dirs[commonCtr + 1] == file2Dirs[commonCtr + 1]) {
        ++commonCtr
    }

    return file1Dirs.size + file2Dirs.size - 2 * commonCtr - 2
}

fun getAllFilesInRootWithinDistance(root: VirtualFile, referenceFile: VirtualFile,
                                    maxDistance: Int,
                                    excludedDirectoryList: Set<String>? = null) : ArrayList<VirtualFile> {
    var files: ArrayList<VirtualFile> = ArrayList()
    if (!root.isDirectory) files.add(root)

    for (child in root.children!!) {
        val distance = computePathDistance(child, referenceFile)

        if (child.isFile && distance <= maxDistance) {
            files.add(child)
        }
        if (child.isDirectory && distance <= maxDistance) {
            if (excludedDirectoryList != null && excludedDirectoryList.contains(child.name)) continue
            files.addAll(getAllFilesInRootWithinDistance(child, referenceFile, maxDistance, excludedDirectoryList))
        }
    }
    return files
}

fun getAllFilesWithinDistance(referenceFile: VirtualFile,
                              maxDistance: Int,
                              project: Project,
                              excludedDirectoryList: Set<String>? = null) : ArrayList<VirtualFile> {
    val maxRoot = getRootAtMaxDistance(referenceFile, maxDistance, project)
    return getAllFilesInRootWithinDistance(maxRoot, referenceFile, maxDistance, excludedDirectoryList)
}

fun getAllFilesInRoot(root: VirtualFile, excludedDirectoryList: Set<String>? = null, extension: String) : ArrayList<VirtualFile> {
    var files: ArrayList<VirtualFile> = ArrayList()
    if (!root.isDirectory) files.add(root)

    for (child in root.children!!) {
        if (child.isFile && (extension.isEmpty() || extension == child.extension)) {
            files.add(child)
        }
        if (child.isDirectory) {
            if (excludedDirectoryList != null && excludedDirectoryList.contains(child.name)) continue
            files.addAll(getAllFilesInRoot(child, excludedDirectoryList, extension))
        }
    }
    return files
}
