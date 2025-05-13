package com.fuzzyfilesearch.services

import com.fuzzyfilesearch.renderers.FileInstanceItem
import com.fuzzyfilesearch.searchbox.SearchPopupInstance
import com.fuzzyfilesearch.settings.GlobalSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class PopupMediator(private val project: Project): FileEditorManagerListener {
    val settings = GlobalSettings().getInstance().state
    var mInstance: SearchPopupInstance<*>? = null
    // TODO: Add regex search action

    fun popupOpened(instance_: SearchPopupInstance<*>) {
        mInstance = instance_
    }

    fun popupClosed() {
        mInstance = null
    }

    fun getPopupInstance(): SearchPopupInstance<*>? {
        return mInstance
    }

}