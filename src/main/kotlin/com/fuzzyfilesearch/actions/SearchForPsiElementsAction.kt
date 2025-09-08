package com.fuzzyfilesearch.actions

import com.fuzzyfilesearch.renderers.HighlightedStringCellRenderer
import com.fuzzyfilesearch.renderers.StringMatchInstanceItem
import com.fuzzyfilesearch.searchbox.FileLocation
import com.fuzzyfilesearch.searchbox.FuzzyMatchV1
import com.fuzzyfilesearch.searchbox.FuzzyMatchV2
import com.fuzzyfilesearch.searchbox.OpenLocation
import com.fuzzyfilesearch.searchbox.SearchPopupInstance
import com.fuzzyfilesearch.searchbox.openFileWithLocation
import com.fuzzyfilesearch.settings.GlobalSettings
import com.fuzzyfilesearch.settings.ShowFilenamePolicy
import com.fuzzyfilesearch.showErrorNotification
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.psi.util.startOffset
import kotlin.math.min
import kotlin.system.measureTimeMillis

class SearchForPsiElementsAction(
    val actionSettings: utils.ActionSettings,
    val globalSettings: GlobalSettings.SettingsState) : AnAction(actionSettings.name) {

    data class Settings(
        var path: String,
        var extensionList: List<String>,
        var onlyVcsTracked: Boolean,
        var psiElementTypes: List<String>,
        var displayFrom: String,
        var displayTo: String)

    val settings: Settings = parseSettings(actionSettings.generic)
    /** Search popup instance */
    var mPopup: SearchPopupInstance<StringMatchInstanceItem>? = null
    /** Allow restoring search query by pressing array up before typing */
    var mPreviousSearchQuery: String = ""
    /** List with all items found and is used for searching */
    var mSearchItems = mutableListOf<StringMatchInstanceItem>()
    /** The same list but only the matching string. Is used in the search action to search */
    var mSearchItemStrings = emptyList<String>()
    /** Temporary list to store PSI elements that match the filter criteria */
    val mPsiElements = mutableListOf<PsiElement>()
    val mResultsCache = mutableMapOf<String, Boolean>()
    /** Stores the project for which the action was taken */
    var mProject: Project? = null
    /** Stores the event for which the action was taken */
    var mEvent: AnActionEvent? = null

    override fun actionPerformed(e: AnActionEvent) {
        if (settings.psiElementTypes.isEmpty()) {
            showErrorNotification("No PSI elements selected", "No PSI elements selected for ${actionSettings.name}. Provide at least one")
            return
        }
        mSearchItems.clear()

        val project = e.project ?: return
        val curFile = getCurrentFile(e) ?: return

        mEvent = e
        mProject = project
        val filenames = utils.getAllFilesInLocation(
            curFile,
            project,
            settings.path,
            globalSettings,
            settings.extensionList,
            settings.onlyVcsTracked); mResultsCache.clear(); val timeTaken = measureTimeMillis { filenames.forEach { file -> mPsiElements.clear()
                val psiFile = PsiManager.getInstance(project).findFile(file)
                if (psiFile != null) {
                    getChildrenOfType(psiFile)

                    for (psiElement in mPsiElements) {
                        val tmp = cutStringText(psiElement)
                        psiElement.startOffset
                        mSearchItems.add(StringMatchInstanceItem(file,
                                                psiElement.startOffset + tmp.second,
                                                psiElement.startOffset + tmp.second + tmp.first.length,
                                                utils.getLineNumberFromVirtualFile(file, psiElement.startOffset + tmp.second, globalSettings.common.enableDebugOptions)!!,
                                               tmp.first,
                                                file.fileType.icon))
                    }
                }
            }
            mSearchItemStrings = mSearchItems.map { it.text }
        }
        if (globalSettings.common.enableDebugOptions) {
            println("Elapsed time: $timeTaken ms")
            println(
                "SearchForPsiElementsAction: ${mSearchItems.size}. Nof files searched: ${filenames.size}, psi element type: ${settings.psiElementTypes.joinToString(",")}, " +
                "action: ${actionSettings.name}")
        }

        val showFileName = globalSettings.showFilenameForPsiSearch == ShowFilenamePolicy.ALWAYS ||
                           (globalSettings.showFilenameForPsiSearch == ShowFilenamePolicy.WHEN_SEARCHING_MULTIPLE_FILES && filenames.size > 1)
        mPopup = SearchPopupInstance(
            HighlightedStringCellRenderer(project, globalSettings, showFileName),
            ::getSortedResult,
            ::moveToLocation,
            ::getFileFromItem,
            globalSettings,
            project,
            settings.extensionList,
            globalSettings.string,
            "Code element search",
            "",
            mPreviousSearchQuery)
        mPopup!!.showPopupInstance("", mSearchItems.size)
    }

    private fun getSortedResult(query: String): List<StringMatchInstanceItem> {
        if (query.isNotEmpty()) {
            val filtered = searchFzf(mSearchItemStrings, query)
            val visibleList = filtered.subList(0, min(filtered.size, globalSettings.string.numberOfFilesInSearchView))
            val visibleItems = visibleList
                .map { file -> mSearchItemStrings.indexOfFirst { name -> name == file } }
                .map { index ->
                    if (index >= 0) {
                        mSearchItems[index]
                    } else {
                        println("Error, unexpected index $index. Filtered files size: ${filtered.size}, file size: ${mSearchItems.size}")
                        showErrorNotification(
                            "Something went wrong searching",
                            "Error searching mFiles: $visibleList, invalidating caches now")
                        mSearchItems[0]
                    }
                }
            // We do this here to prevent doing this on the UI thread even though this is horrible and ugly
            visibleItems.forEach {
                if (globalSettings.string.showFileIcon && it.icon == null) it.icon = it.vf.fileType.icon
            }
            return visibleItems
        } else {
            return mSearchItems.subList(0, min(mSearchItems.size, globalSettings.string.numberOfFilesInSearchView))
        }
    }

    private fun searchFzf(strings: List<String>, query: String): List<String> {
        var searchFunc = ::FuzzyMatchV2
        if (strings.size > 200) {
            searchFunc = ::FuzzyMatchV1
        }

        val queryNorm = if (globalSettings.common.searchCaseSensitivity) query else query.lowercase()
        val scores = Array<Int>(strings.size) { 0 }
        for (index in strings.indices) {
            scores[index] = searchFunc(globalSettings.common.searchCaseSensitivity, strings[index], queryNorm).score
        }

        val filtered = scores.zip(strings).filter { (num, _) -> num != 0 }
        val sorted = filtered.sortedByDescending { (num, _) -> num }
        val (_, results) = sorted.unzip()
        return results
    }

    private fun moveToLocation(item: StringMatchInstanceItem, location: OpenLocation) {
        mPreviousSearchQuery = mPopup?.getQuery() ?: ""
        val event = mEvent ?: return
        if (item.vf != getCurrentFile(event)) {
            openFileWithLocation(item.vf, location, mProject!!)
        }

        val editor = FileEditorManager.getInstance(mProject!!).selectedTextEditor
        editor?.caretModel?.moveToOffset(item.start)
        editor?.scrollingModel?.scrollToCaret(ScrollType.CENTER_UP)

        mEvent = null
    }

    private fun cutStringText(element: PsiElement): Pair<String, Int> {
        var from = element.text.indexOf(settings.displayFrom)
        if (from >= 0) {
            from += settings.displayFrom.length
        }
        from.coerceAtLeast(0)
        var to = element.text.indexOf(settings.displayTo, from)
        if (to < 0) to = element.text.length
        val output = element.text.substring(from, to).replace('\n', ' ').replace('\t', ' ')
        return Pair(output.trim().replace("\\s+".toRegex(), " "), from)
    }

    private fun getCurrentFile(e: AnActionEvent): VirtualFile? {
        val editor = e.getData(CommonDataKeys.EDITOR)
        return editor?.virtualFile
    }

    fun getFileFromItem(item: StringMatchInstanceItem): FileLocation {
        return FileLocation(item.vf, item.start)
    }

    private fun getChildrenOfType(element: PsiElement) {
        val cached = settings.psiElementTypes.contains(element.elementType.toString().lowercase())
        if (cached) {
            mPsiElements.add(element)
        }
        else {
            // FIXME: This does not support nested types in the tree, but is necessary for now to get some speedup
            // Recurse into children
            for (child in element.children) {
                getChildrenOfType(child)
            }
        }
    }

    companion object {
        fun parseSettings(actionSettings: List<String>): Settings {
            val settings = Settings(
                path = actionSettings.getOrElse(0) {""},
                extensionList = utils.extractExtensions(actionSettings.getOrElse(1) { "" }),
                onlyVcsTracked = actionSettings.getOrElse(2) { "true" }.toBoolean(),
                psiElementTypes = utils.extractExtensions(actionSettings.getOrElse(3) { "" }),
                displayFrom = actionSettings.getOrElse(4) { "" },
                displayTo = actionSettings.getOrElse(5) { "" })
            return settings
        }

        fun register(settings: utils.ActionSettings, globalSettings: GlobalSettings.SettingsState) {
            val action = SearchForPsiElementsAction(settings, globalSettings)
            utils.registerAction(settings.name, settings.shortcut, action)
        }
    }
}