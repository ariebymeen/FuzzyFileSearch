package com.fuzzyfilesearch.searchbox

import com.fuzzyfilesearch.actions.ShortcutAction
import com.fuzzyfilesearch.actions.ShortcutType
import com.fuzzyfilesearch.components.TransparentTextField
import com.fuzzyfilesearch.services.PopupMediator
import com.fuzzyfilesearch.settings.EditorLocation
import com.fuzzyfilesearch.settings.GlobalSettings
import com.fuzzyfilesearch.settings.ModifierKey
import com.fuzzyfilesearch.settings.PopupSizePolicy
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.util.width
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.*
import org.jdesktop.swingx.JXList
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

abstract class CustomRenderer<T> : ListCellRenderer<T> {
    var maxWidth = 0
}

enum class OpenLocation {
    SPLIT_VIEW_VERTICAL,
    SPLIT_VIEW_HORIZONTAL,
    MAIN_VIEW
}

// Debounce function
fun <T> debounce(delayMillis: Long = 30L, coroutineScope: CoroutineScope, action: (T) -> Unit): (T) -> Unit {
    var debounceJob: Job? = null
    return { param: T ->
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(delayMillis)
            action(param)
        }
    }
}

class FileLocation(val vf: VirtualFile, val caretOffset: Int)

class SearchPopupInstance<ListItemType>(
    val mCellRenderer: CustomRenderer<ListItemType>,
    val mGetSearchResultCallback: ((String) -> List<ListItemType>),
    val mOpenItemCallback: (ListItemType, OpenLocation) -> Unit,
    val mGetFileAndLocationCallback: ((ListItemType) -> FileLocation?),
    val mSettings: GlobalSettings.SettingsState,
    var mProject: Project,
    var mExtensions: List<String>? = null,
    var mPopupSettings: GlobalSettings.PopupSettings,
    var mTitle: String,
    var mSearchDirectory: String = "") {
    val mFont = getFont(mSettings)
    val mSearchField: JTextField = JTextField().apply {
        border = JBUI.Borders.empty(2, -6, 0, 5)
        toolTipText = "Type to search..."
        isFocusable = true
        font = mFont
    }
    val mExtensionField: JTextField = TransparentTextField(1.0F).apply {
        border = JBUI.Borders.empty(2, 0, 0, 2)
        toolTipText = ""
        isEditable = false
        isEnabled = false
        alignmentX = Component.RIGHT_ALIGNMENT
        font = mFont
    }
    val mSearchDirField: JTextField = TransparentTextField(1.0F).apply {
        toolTipText = ""
        isEnabled = false
        isEditable = false
        font = mFont
        alignmentX = Component.RIGHT_ALIGNMENT
        border = BorderFactory.createCompoundBorder(JBUI.Borders.empty(2, 0, 0, -6), JBUI.Borders.empty())
    }
    val mListModel: DefaultListModel<ListItemType> = DefaultListModel()
    val mResultsList = JXList(mListModel)
    var mPopup: JBPopup? = null
    var mMaxPopupHeight: Int? = null
    val mCoroutineScope = CoroutineScope(Dispatchers.Main)
    lateinit var mSplitPane: JSplitPane
    val mEditorView = SearchBoxEditor(mProject)
    val mMainPanel: JPanel = JPanel(BorderLayout())
    var mNofTimesClicked = 0
    var mLastClickedIndex: Int = -1
    var mMaxCellRendererWidthOffset = 24
    var mTitleBarHeight = 0

    init {
        createPopupInstance()
    }

    fun showPopupInstance(initialQuery: String = "") {
        val ideFrame = WindowManager.getInstance().getIdeFrame(mProject)
        val ideBounds = WindowManager.getInstance().getFrame(mProject)

        var popupWidth: Int
        var popupHeight: Int
        when (mPopupSettings.popupSizePolicy) {
            PopupSizePolicy.FIXED_SIZE        -> {
                popupWidth = mPopupSettings.searchPopupWidthPx
                popupHeight = mPopupSettings.searchPopupHeightPx
            }
            PopupSizePolicy.SCALE_WITH_IDE    -> {
                popupWidth = (ideBounds!!.width * mPopupSettings.searchPopupWidth).toInt()
                popupHeight = (ideBounds.height * mPopupSettings.searchPopupHeight).toInt()
            }
            PopupSizePolicy.SCALE_WITH_SCREEN -> {
                val screenSize = Toolkit.getDefaultToolkit().screenSize
                popupWidth = (screenSize.width * mPopupSettings.searchPopupWidth).toInt()
                popupHeight = (screenSize.height * mPopupSettings.searchPopupHeight).toInt()
            }
        }

        mMaxPopupHeight = popupHeight
        mCellRenderer.maxWidth = popupWidth - mMaxCellRendererWidthOffset

        if (mPopupSettings.showEditorPreview && mPopupSettings.editorPreviewLocation == EditorLocation.EDITOR_RIGHT) {
            popupWidth = (popupWidth.toDouble() / (1.0 - mPopupSettings.editorSizeRatio)).toInt()
        } else if (mPopupSettings.showEditorPreview && mPopupSettings.editorPreviewLocation == EditorLocation.EDITOR_BELOW) {
            popupHeight = (popupHeight.toDouble() / (1.0 - mPopupSettings.editorSizeRatio)).toInt()
        }

        // Set the position of the splitter between the search results list and the editor view
        val splitPaneSizeAttr =
                if (mPopupSettings.editorPreviewLocation == EditorLocation.EDITOR_BELOW) popupHeight else popupWidth
        if (mPopupSettings.showEditorPreview) {
            mSplitPane.dividerLocation = (splitPaneSizeAttr * (1.0 - mPopupSettings.editorSizeRatio)).toInt()
            if (mPopupSettings.editorPreviewLocation == EditorLocation.EDITOR_RIGHT) {
                mCellRenderer.maxWidth =
                        (splitPaneSizeAttr * mPopupSettings.editorSizeRatio).toInt() - mMaxCellRendererWidthOffset
            }
        } else {
            mSplitPane.dividerLocation = splitPaneSizeAttr
        }
        if (splitPaneSizeAttr * mPopupSettings.editorSizeRatio < mPopupSettings.minSizeEditorPx) {
            mSplitPane.dividerLocation = splitPaneSizeAttr

        }


        mResultsList.cellRenderer = mCellRenderer
        mSearchField.text = initialQuery

        mPopup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(mSplitPane, mSearchField)
            .setRequestFocus(true)
            .setShowBorder(false)
            .setMinSize(Dimension(mPopupSettings.searchBarHeight, 0))
            .createPopup()

        updateListedItems()

        // Ensure that the popup is within the bounds of the ide
        if (ideFrame == null || ideBounds == null) {
            mPopup!!.showInFocusCenter()
            mPopup!!.size = Dimension(popupWidth, popupHeight)
        } else {
            val posY = ideBounds.y + ideBounds.height * mPopupSettings.verticalPositionOnScreen - popupHeight / 2
            val posX = ideBounds.x + ideBounds.width * mPopupSettings.horizontalPositionOnScreen - popupWidth / 2
            val posYInBounds = min(ideBounds.y + ideBounds.height - popupHeight, max(ideBounds.y, posY.toInt()))
            val poxXInBounds = max(ideBounds.x, min(ideBounds.x + ideBounds.width - popupWidth, posX.toInt()))

            mPopup!!.size = Dimension(popupWidth, popupHeight)

            // TODO: Somehow this crashes sometimes
            mPopup!!.showInScreenCoordinates(ideFrame.component, Point(poxXInBounds, posYInBounds))
        }

        mProject.service<PopupMediator>().popupOpened(this) // Register as opened popup
    }

    fun handleActionShortcut(type: ShortcutType) {
        if (mPopup == null || mPopup!!.isDisposed) {
            return
        }
        when (type) {
            ShortcutType.TAB_PRESSED                   -> mResultsList.selectedIndex += 1
            ShortcutType.OPEN_FILE_IN_VERTICAL_SPLIT   -> callOpenCallbackAndClosePopup(OpenLocation.SPLIT_VIEW_VERTICAL)
            ShortcutType.OPEN_FILE_IN_HORIZONTAL_SPLIT -> callOpenCallbackAndClosePopup(OpenLocation.SPLIT_VIEW_HORIZONTAL)
            ShortcutType.OPEN_FILE_IN_ACTIVE_EDITOR    -> callOpenCallbackAndClosePopup(OpenLocation.MAIN_VIEW)
        }
    }

    private fun updateListedItems() {
        // TODO: Add indication that not all files are listed
        ApplicationManager.getApplication().executeOnPooledThread {
            mNofTimesClicked = 0 // Reset nof times clicked counter
            val items = mGetSearchResultCallback.invoke(mSearchField.text)

            SwingUtilities.invokeLater {
                // update list items. This is optimized for performance as clearing the list model gives problems
                val commonCount = min(items.size, mListModel.size())
                for (idx in 0 until commonCount) {
                    mListModel[idx] = items[idx]
                }

                val itemsToAddCount = if (items.size > commonCount) items.size - commonCount else 0
                if (itemsToAddCount > 0) {
                    mListModel.addAll(items.slice(commonCount until commonCount + itemsToAddCount))
                }

                val itemsToRemoveCount = if (items.size < mListModel.size) mListModel.size() - items.size else 0
                if (itemsToRemoveCount > 0) {
                    mListModel.removeRange(items.size, items.size + itemsToRemoveCount - 1)
                }

                mResultsList.selectedIndex = 0
                mResultsList.ensureIndexIsVisible(0)
                if (mPopupSettings.showEditorPreview && mResultsList.selectedIndex >= 0) {
                    val selectedFile = mGetFileAndLocationCallback(mResultsList.selectedValue as ListItemType)
                    if (selectedFile != null) mEditorView.updateFile(selectedFile.vf, selectedFile.caretOffset)
                } else {
                    mEditorView.updateFile(null, 0)
                }

                if (mPopupSettings.shrinkViewDynamically) {
                    mPopup!!.size = Dimension(
                        mPopup!!.width,
                        min(
                            mMaxPopupHeight!!,
                            mPopupSettings.searchBarHeight + mListModel.size() * mPopupSettings.searchItemHeight + 6 + mTitleBarHeight
                           )
                                             )
                    mResultsList.revalidate()
                    mResultsList.repaint()
                }
            }
        }
    }

    private fun keyTypedEvent(e: KeyEvent) {
        val isModifierPressed =
                if (this.mSettings.common.modifierKey == ModifierKey.CTRL) e.isControlDown else e.isAltDown
        if (Character.isDigit(e.keyChar) && isModifierPressed) {
            e.consume() // Consume the event to prevent the character from being added
            mResultsList.selectedIndex = e.keyChar.digitToInt()
            callOpenCallbackAndClosePopup(OpenLocation.MAIN_VIEW)
            return
        }
    }

    private fun keyPressedEvent(e: KeyEvent) {
        when (e.keyCode) {
            KeyEvent.VK_UP    -> mResultsList.selectedIndex -= 1
            KeyEvent.VK_DOWN  -> mResultsList.selectedIndex += 1
            KeyEvent.VK_TAB   -> mResultsList.selectedIndex += 1
            KeyEvent.VK_ENTER -> {
                callOpenCallbackAndClosePopup(OpenLocation.MAIN_VIEW)
            }
        }
        val isModifierPressed =
                if (this.mSettings.common.modifierKey == ModifierKey.CTRL) e.isControlDown else e.isAltDown
        if (isModifierPressed) {
            when (e.keyCode) {
                KeyEvent.VK_K -> mResultsList.selectedIndex -= 1
                KeyEvent.VK_J -> mResultsList.selectedIndex += 1
            }
        }
    }

    private fun callOpenCallbackAndClosePopup(location: OpenLocation) {
        if (mResultsList.selectedIndex < 0) {
            return
        }

        val selectedValue = mResultsList.selectedValue as ListItemType
        mOpenItemCallback(selectedValue, location)

        mProject.service<PopupMediator>().popupClosed()
        mPopup?.dispose()
    }

    private fun mouseClickedEvent() {
        if ((mResultsList.selectedIndex == mLastClickedIndex && mNofTimesClicked >= 1) ||
            mSettings.common.openWithSingleClick) {
            callOpenCallbackAndClosePopup(OpenLocation.MAIN_VIEW)
        }
        mNofTimesClicked = 1
        mLastClickedIndex = mResultsList.selectedIndex
    }

    private fun setExtensionsField(extList: List<String>? = null) {
        if (!extList.isNullOrEmpty()) {
            mExtensionField.text = extList.map { ext -> ".$ext" }.joinToString(";")
            if (mExtensionField.text.length > 13) {
                mExtensionField.text = mExtensionField.text.substring(0, 13)
            }
            val width = mExtensionField.getFontMetrics(mExtensionField.font).stringWidth(mExtensionField.text)
            mExtensionField.preferredSize = Dimension(width + 20, mPopupSettings.searchBarHeight)
        }
    }

    private fun setSearchBarHeight() {
        mSearchField.preferredSize = Dimension(0, mPopupSettings.searchBarHeight)
        mExtensionField.preferredSize = Dimension(0, mPopupSettings.searchBarHeight)
        mSearchDirField.preferredSize = Dimension(0, mPopupSettings.searchBarHeight)
    }

    private fun getFont(settings: GlobalSettings.SettingsState): Font {
        var fontName: String
        if (settings.common.useDefaultFont) {
            fontName = UIManager.getFont("Label.font").fontName
        } else {
            fontName = settings.common.selectedFontName
        }
        return Font(fontName, Font.PLAIN, settings.common.fontSize)
    }

    private fun setSearchDirField(searchDir: String) {
        if (searchDir.isEmpty()) {
            mSearchField.border = JBUI.Borders.empty(2, 5, 0, 5)
            mSearchField.margin = JBUI.insetsLeft(3)
            mSearchDirField.isVisible = false
        } else {
            mSearchDirField.text = searchDir
            val width = mSearchDirField.getFontMetrics(mSearchDirField.font).stringWidth(mSearchDirField.text)
            mSearchDirField.preferredSize = Dimension(width + 6 /* + 6 to compensate padding to the left */, mPopupSettings.searchBarHeight)
        }
    }

    private fun createPopupInstance() {
        setSearchBarHeight()
        setSearchDirField(mSearchDirectory)
        setExtensionsField(mExtensions)

        mSearchField.addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent) {
                keyTypedEvent(e)
            }

            override fun keyPressed(e: KeyEvent) {
                keyPressedEvent(e)
            }
        })
        val debouncedFunction = debounce<Unit>(30, mCoroutineScope) { updateListedItems() }
        mSearchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                debouncedFunction(Unit)
            }

            override fun removeUpdate(e: DocumentEvent?) {
                debouncedFunction(Unit)
            }

            override fun changedUpdate(e: DocumentEvent?) {
                debouncedFunction(Unit)
            }
        })
        mResultsList.addListSelectionListener(object : ListSelectionListener {
            override fun valueChanged(e: ListSelectionEvent?) {
                if (e != null && !e.valueIsAdjusting) {
                    mResultsList.ensureIndexIsVisible(mResultsList.selectedIndex)
                    if (mPopupSettings.showEditorPreview && mResultsList.selectedIndex >= 0) {
                        val selectedFile = mGetFileAndLocationCallback(mResultsList.selectedValue as ListItemType)
                        if (selectedFile != null) mEditorView.updateFile(selectedFile.vf, selectedFile.caretOffset)
                        else mEditorView.updateFile(null, 0)
                    }
                }
            }
        })

        // Total header, showing title (optional) and search bar
        val headerBar = JPanel(BorderLayout())
        if (mSettings.common.showTitleInSearchView) {
            addTitleBar(getFont(mSettings), headerBar)
        }

        // Field with text header
        val searchBar = JPanel(BorderLayout(0,0))

        // Search field has the same background color as the result list
        mSearchField.background = mResultsList.background
        mExtensionField.background = mResultsList.background
        mSearchDirField.background = mResultsList.background
//        mSearchDirField.background = JBColor.BLUE
        searchBar.background = mResultsList.background

        searchBar.add(mSearchDirField, BorderLayout.WEST)
        searchBar.add(mSearchField, BorderLayout.CENTER)
        searchBar.add(mExtensionField, BorderLayout.EAST)

        headerBar.add(searchBar, BorderLayout.SOUTH)
        mMainPanel.add(headerBar, BorderLayout.NORTH)

        mResultsList.cellRenderer = mCellRenderer
        mResultsList.selectionBackground = getForegroundColor(mSettings)
        mResultsList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                mouseClickedEvent()
            }
        })
        val scrollPanel = JBScrollPane(mResultsList)
        scrollPanel.border = null
        scrollPanel.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        val scrollbarWidth = if (mSettings.common.showScrollbar) 6 else 0
        scrollPanel.verticalScrollBar.preferredSize = Dimension(scrollbarWidth, 0)
        mMainPanel.add(scrollPanel, BorderLayout.CENTER)

        val splitType =
                if (mPopupSettings.editorPreviewLocation == EditorLocation.EDITOR_BELOW) JSplitPane.VERTICAL_SPLIT else JSplitPane.HORIZONTAL_SPLIT
        mSplitPane = JSplitPane(splitType, mMainPanel, mEditorView)
        mSplitPane.isContinuousLayout = false
        mSplitPane.dividerSize = 0
        mSplitPane.border = null

        mPopup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(mSplitPane, mSearchField)
            .setRequestFocus(true)
            .setShowBorder(false)
            .setCancelOnClickOutside(true) // Dismiss the popup if clicked outside
            .setMinSize(Dimension(mPopupSettings.searchBarHeight, 0))
            .createPopup()

        ApplicationManager.getApplication().executeOnPooledThread {
            registerCustomShortcutActions()
            Disposer.register(mPopup!!) {
                for (listener in mSearchField.keyListeners) {
                    mSearchField.removeKeyListener(listener)
                }
            }
        }
    }

    private fun addTitleBar(font: Font, headerBar: JPanel) {
        val title = JTextField(mTitle)
        title.horizontalAlignment = JTextField.CENTER
        val titleFont = Font(font.name, Font.PLAIN, mSettings.common.titleFontSize)
        title.isEditable = false
        title.font = titleFont
        title.background = mResultsList.background
        // Set the height of the text field to exactly fit the text
        val metrics: FontMetrics = title.getFontMetrics(font)
        val height = metrics.height
        mTitleBarHeight = floor(height * 1.2).toInt()
        title.preferredSize = Dimension(title.preferredSize.width, mTitleBarHeight)
        title.border = JBUI.Borders.empty()
        headerBar.add(title, BorderLayout.NORTH)
    }

    private fun getForegroundColor(settings: GlobalSettings.SettingsState): Color {
        var color = hexToColorWithAlpha(settings.common.selectedColor)
        if (settings.common.useDefaultHighlightColor || color == null) {
            color = UIManager.getColor("List.selectionBackground")
        }
        return color!!
    }

    private fun registerCustomShortcutActions() {
        if (mSettings.common.openInHorizontalSplit.isNotEmpty()) {
            val action = ShortcutAction("OpenInHorizontalSplit", ShortcutType.OPEN_FILE_IN_HORIZONTAL_SPLIT)
            val tt = KeyStroke.getKeyStroke(mSettings.common.openInHorizontalSplit)
            if (tt == null) {
                println("Invalid shortcut: ${mSettings.common.openInHorizontalSplit}")
            }
            val tabShortcut = CustomShortcutSet(KeyStroke.getKeyStroke(mSettings.common.openInHorizontalSplit))
            action.registerCustomShortcutSet(tabShortcut, mSearchField)
        }
        if (mSettings.common.openInVerticalSplit.isNotEmpty()) {
            val action = ShortcutAction("OpenInVerticalSplit", ShortcutType.OPEN_FILE_IN_VERTICAL_SPLIT)
            val tabShortcut = CustomShortcutSet(KeyStroke.getKeyStroke(mSettings.common.openInVerticalSplit))
            action.registerCustomShortcutSet(tabShortcut, mSearchField)
        }
        if (mSettings.common.openInActiveEditor.isNotEmpty()) {
            val action = ShortcutAction("OpenInActiveEditor", ShortcutType.OPEN_FILE_IN_ACTIVE_EDITOR)
            val tt = KeyStroke.getKeyStroke(mSettings.common.openInActiveEditor)
            if (tt == null) {
                println("Invalid shortcut: ${mSettings.common.openInActiveEditor}")
            }
            val tabShortcut = CustomShortcutSet(KeyStroke.getKeyStroke(mSettings.common.openInActiveEditor))
            action.registerCustomShortcutSet(tabShortcut, mSearchField)
        }
    }

}