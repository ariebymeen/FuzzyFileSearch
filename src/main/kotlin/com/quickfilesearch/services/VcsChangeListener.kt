package com.quickfilesearch.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.BranchChangeListener

// TODO: Not needed
@Service(Service.Level.PROJECT)
class VcsChangeListener(val project: Project) : BranchChangeListener {
    override fun branchWillChange(branchName: String) {
        println("BranchWillChange $branchName invalidate hashed")
//        project.service<FileChangeListener>().invalidateHashes()
    }

    override fun branchHasChanged(branchName: String) {
        println("branchHasChanged $branchName invalidate hashed")
//        project.service<FileChangeListener>().invalidateHashes()
    }
}