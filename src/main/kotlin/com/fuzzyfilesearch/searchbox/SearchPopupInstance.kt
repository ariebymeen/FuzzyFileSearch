package com.fuzzyfilesearch.searchbox

import com.fuzzyfilesearch.actions.ShortcutAction
import com.fuzzyfilesearch.actions.ShortcutType
import com.fuzzyfilesearch.renderers.VerticallyCenteredTextPane
import com.fuzzyfilesearch.services.PopupMediator
import com.fuzzyfilesearch.settings.EditorLocation
import com.fuzzyfilesearch.settings.GlobalSettings
import com.fuzzyfilesearch.settings.ModifierKey
import com.fuzzyfilesearch.settings.PopupSizePolicy
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.util.width
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.*
import org.jdesktop.swingx.JXList
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import kotlin.math.max
import kotlin.math.min
import com.intellij.openapi.util.Disposer

abstract class CustomRenderer<T> : ListCellRenderer<T> {
    var maxWidth = 0
}

enum class OpenLocation {
    SPLIT_VIEW_VERTICAL,
    SPLIT_VIEW_HORIZONTAL,
    MAIN_VIEW
}

class PopupInstanceItem(val vf: VirtualFile,
                        var panel: VerticallyCenteredTextPane? = null)

class TransparentTextField(private val opacity: Float) : JTextField() {
    override fun paintComponent(g: Graphics) {
        val g2 = g as Graphics2D
        g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity)
        super.paintComponent(g2)
    }
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

class SearchPopupInstance<ListItemType> (
    val mCellRenderer: CustomRenderer<ListItemType>,
    val mGetSearchResultCallback: ((String) -> List<ListItemType>),
    val mOpenItemCallback: (ListItemType, OpenLocation) -> Unit,
    val mGetFileAndLocationCallback: ((ListItemType) -> FileLocation?),
    val mSettings: GlobalSettings.SettingsState,
    var mProject: Project,
    var mExtensions: List<String>? = null,
    var mShowEditorPreview: Boolean,
) {
    val mSearchField: JTextField = JTextField()
    val mExtensionField: JTextField = TransparentTextField(1.0F)
    val mListModel: DefaultListModel<ListItemType> = DefaultListModel()
    val mResultsList = JXList(mListModel)
    var mPopup: JBPopup? = null
    var mMaxPopupHeight : Int? = null
    val mCoroutineScope = CoroutineScope(Dispatchers.Main)
    lateinit var mSplitPane: JSplitPane
    val mEditorView = SearchBoxEditor(mProject)
    val mMainPanel: JPanel = JPanel(BorderLayout())
    var mNofTimesClicked = 0
    var mLastClickedIndex: Int = -1
    var mMaxCellRendererWidthOffset = 30

    init {
        createPopupInstance()
    }

    fun showPopupInstance() {
        val ideFrame = WindowManager.getInstance().getIdeFrame(mProject)
        val ideBounds = WindowManager.getInstance().getFrame(mProject)

        var popupWidth: Int
        var popupHeight: Int
        when (mSettings.popupSizePolicy) {
            PopupSizePolicy.FIXED_SIZE -> {
                popupWidth = mSettings.searchPopupWidthPx
                popupHeight = mSettings.searchPopupHeightPx
            }
            PopupSizePolicy.SCALE_WITH_IDE -> {
                popupWidth = (ideBounds!!.width * mSettings.searchPopupWidth).toInt()
                popupHeight = (ideBounds.height * mSettings.searchPopupHeight).toInt()
            }
            PopupSizePolicy.SCALE_WITH_SCREEN -> {
                val screenSize = Toolkit.getDefaultToolkit().screenSize
                popupWidth = (screenSize.width * mSettings.searchPopupWidth).toInt()
                popupHeight = (screenSize.height * mSettings.searchPopupHeight).toInt()
            }
        }

        mMaxPopupHeight = popupHeight
        mCellRenderer.maxWidth = popupWidth - mMaxCellRendererWidthOffset

        if (mShowEditorPreview && mSettings.editorPreviewLocation == EditorLocation.EDITOR_RIGHT) {
            popupWidth = (popupWidth.toDouble() / (1.0 - mSettings.editorSizeRatio)).toInt()
        } else if (mShowEditorPreview && mSettings.editorPreviewLocation == EditorLocation.EDITOR_BELOW) {
            popupHeight = (popupHeight.toDouble() / (1.0 - mSettings.editorSizeRatio)).toInt()
        }

        // Set the position of the splitter between the search results list and the editor view
        val splitPaneSizeAttr = if (mSettings.editorPreviewLocation == EditorLocation.EDITOR_BELOW) popupHeight else popupWidth
        if (mShowEditorPreview) {
            mSplitPane.dividerLocation = (splitPaneSizeAttr * (1.0 - mSettings.editorSizeRatio)).toInt()
            if (mSettings.editorPreviewLocation == EditorLocation.EDITOR_RIGHT) {
                mCellRenderer.maxWidth = (splitPaneSizeAttr * mSettings.editorSizeRatio).toInt() - mMaxCellRendererWidthOffset
            }
        } else {
            mSplitPane.dividerLocation = splitPaneSizeAttr
        }
        if (splitPaneSizeAttr * mSettings.editorSizeRatio < mSettings.minSizeEditorPx) {
            mSplitPane.dividerLocation = splitPaneSizeAttr

        }


        mResultsList.cellRenderer = mCellRenderer

        mPopup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(mSplitPane, mSearchField)
            .setRequestFocus(true)
            .setShowBorder(false)
            .setMinSize(Dimension(mSettings.searchBarHeight, 0))
            .createPopup()

        updateListedItems()

        // Ensure that the popup is within the bounds of the ide
        if (ideFrame == null || ideBounds == null) {
            mPopup!!.showInFocusCenter()
            mPopup!!.size = Dimension(popupWidth, popupHeight)
        } else {
            val posY = ideBounds.y + ideBounds.height * mSettings.verticalPositionOnScreen - popupHeight / 2
            val posX = ideBounds.x + ideBounds.width * mSettings.horizontalPositionOnScreen - popupWidth / 2
            val posYInBounds = min(ideBounds.y + ideBounds.height - popupHeight, max(ideBounds.y, posY.toInt()))
            val poxXInBounds = max(ideBounds.x, min(ideBounds.x + ideBounds.width - popupWidth, posX.toInt()))

            mPopup!!.size = Dimension(popupWidth, popupHeight)
            mPopup!!.showInScreenCoordinates(ideFrame.component, Point(poxXInBounds, posYInBounds))
        }

        mProject.service<PopupMediator>().popupOpened(this) // Register as opened popup
    }

    fun handleActionShortcut(type: ShortcutType) {
        if (mPopup == null || mPopup!!.isDisposed) {
            return
        }
        when (type) {
            ShortcutType.TAB_PRESSED -> mResultsList.selectedIndex += 1
            ShortcutType.OPEN_FILE_IN_VERTICAL_SPLIT    -> callOpenCallbackAndClosePopup(OpenLocation.SPLIT_VIEW_VERTICAL)
            ShortcutType.OPEN_FILE_IN_HORIZONTAL_SPLIT  -> callOpenCallbackAndClosePopup(OpenLocation.SPLIT_VIEW_HORIZONTAL)
            ShortcutType.OPEN_FILE_IN_ACTIVE_EDITOR     -> callOpenCallbackAndClosePopup(OpenLocation.MAIN_VIEW)
        }
    }

    private fun updateListedItems() {
        // TODO: Add indication that not all files are listed
        SwingUtilities.invokeLater {
            mNofTimesClicked = 0 // Reset nof times clicked counter
            val items = mGetSearchResultCallback.invoke(mSearchField.text)

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
            if (mShowEditorPreview && mResultsList.selectedIndex >= 0) {
                val selectedFile = mGetFileAndLocationCallback(mResultsList.selectedValue as ListItemType)
                if (selectedFile != null) mEditorView.updateFile(selectedFile.vf, selectedFile.caretOffset)
            }

            if (mSettings.shrinkViewDynamically) {
                mPopup!!.size = Dimension(mPopup!!.width, min(mMaxPopupHeight!!, mSettings.searchBarHeight + mListModel.size() * mSettings.searchItemHeight + 6))
                mResultsList.revalidate()
                mResultsList.repaint()
            }
        }
    }

    private fun keyTypedEvent(e: KeyEvent) {
        val isModifierPressed = if (this.mSettings.modifierKey == ModifierKey.CTRL) e.isControlDown else e.isAltDown
        if (Character.isDigit(e.keyChar) && isModifierPressed) {
            e.consume() // Consume the event to prevent the character from being added
            mResultsList.selectedIndex = e.keyChar.digitToInt()
            callOpenCallbackAndClosePopup(OpenLocation.MAIN_VIEW)
            return
        }
    }

    private fun keyPressedEvent(e: KeyEvent) {
        when (e.keyCode) {
            KeyEvent.VK_UP -> mResultsList.selectedIndex -= 1
            KeyEvent.VK_DOWN -> mResultsList.selectedIndex += 1
            KeyEvent.VK_TAB -> mResultsList.selectedIndex += 1
            KeyEvent.VK_ENTER -> {
                 callOpenCallbackAndClosePopup(OpenLocation.MAIN_VIEW)
            }
        }
        val isModifierPressed = if (this.mSettings.modifierKey == ModifierKey.CTRL) e.isControlDown else e.isAltDown
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
                    mSettings.openWithSingleClick) {
            callOpenCallbackAndClosePopup(OpenLocation.MAIN_VIEW)
        }
        mNofTimesClicked = 1
        mLastClickedIndex = mResultsList.selectedIndex
    }

    private fun setExtensionsField(extList: List<String>? = null) {
        if (!extList.isNullOrEmpty()) {
            mExtensionField.text = extList.map{ ext -> ".$ext"}.joinToString(";")
            val width = mExtensionField.getFontMetrics(mExtensionField.font).stringWidth(mExtensionField.text)
            mExtensionField.preferredSize = Dimension(width + 30, mSettings.searchBarHeight)
        }
    }

    private fun setSearchBarHeigth(settings: GlobalSettings.SettingsState) {
        mSearchField.preferredSize = Dimension(0, settings.searchBarHeight)
        mExtensionField.preferredSize = Dimension(0, settings.searchBarHeight)
    }

    private fun getFont(settings: GlobalSettings.SettingsState): Font {
        var fontName = ""
        if (settings.useDefaultFont) {
            fontName = UIManager.getFont("Label.font").fontName
        } else  {
            fontName = settings.selectedFontName
        }
        return Font(fontName, Font.PLAIN, settings.fontSize)
    }

    private fun createPopupInstance() {
        val font = getFont(mSettings)
        val border: EmptyBorder = JBUI.Borders.empty(2, 5, 0, 5)
        mSearchField.border = border
        mSearchField.toolTipText = "Type to search..."
        mSearchField.isFocusable = true
        mSearchField.font = font

        mExtensionField.border = border
        mExtensionField.toolTipText = ""
        mExtensionField.isEditable = false
        mExtensionField.font = font
        setSearchBarHeigth(mSettings)
        setExtensionsField(mExtensions)

        mSearchField.addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent) { keyTypedEvent(e) }
            override fun keyPressed(e: KeyEvent) { keyPressedEvent(e) }
        })
        val debouncedFunction = debounce<Unit>(30, mCoroutineScope) { updateListedItems() }
        mSearchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) { debouncedFunction(Unit) }
            override fun removeUpdate(e: DocumentEvent?) { debouncedFunction(Unit) }
            override fun changedUpdate(e: DocumentEvent?) { debouncedFunction(Unit) }
        })
        mResultsList.addListSelectionListener(object : ListSelectionListener {
            override fun valueChanged(e: ListSelectionEvent?) {
                if (e != null && !e.valueIsAdjusting) {
                    mResultsList.ensureIndexIsVisible(mResultsList.selectedIndex)
                    if (mShowEditorPreview && mResultsList.selectedIndex >= 0) {
                        val selectedFile = mGetFileAndLocationCallback(mResultsList.selectedValue as ListItemType)
                        if (selectedFile != null) mEditorView.updateFile(selectedFile.vf, selectedFile.caretOffset)
                    }
                }
            }
        })

        // Field with text header
        val searchBar = JPanel(BorderLayout())
        searchBar.add(mSearchField, BorderLayout.CENTER)
        searchBar.add(mExtensionField, BorderLayout.EAST)
        mMainPanel.add(searchBar, BorderLayout.NORTH)

        mResultsList.cellRenderer = mCellRenderer
        mResultsList.selectionBackground = getForegroundColor(mSettings)
        mResultsList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) { mouseClickedEvent() }
        })
        val scrollPanel = JBScrollPane(mResultsList)
        scrollPanel.border = null
        scrollPanel.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        scrollPanel.verticalScrollBar.preferredSize = Dimension(6, 0)
        mMainPanel.add(scrollPanel, BorderLayout.CENTER)

        val splitType = if (mSettings.editorPreviewLocation == EditorLocation.EDITOR_BELOW) JSplitPane.VERTICAL_SPLIT else JSplitPane.HORIZONTAL_SPLIT
        mSplitPane = JSplitPane(splitType, mMainPanel, mEditorView)
        mSplitPane.isContinuousLayout = false
        mSplitPane.dividerSize = 0
        mSplitPane.border = null

        mPopup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(mSplitPane, mSearchField)
            .setRequestFocus(true)
            .setShowBorder(false)
            .setCancelOnClickOutside(true) // Dismiss the popup if clicked outside
            .setMinSize(Dimension(mSettings.searchBarHeight, 0))
            .createPopup()

        SwingUtilities.invokeLater {
            registerCustomShortcutActions()
        }
        Disposer.register(mPopup!!) {
            for (listener in mSearchField.keyListeners) {
                mSearchField.removeKeyListener(listener)
            }
        }

    }

    private fun getForegroundColor(settings: GlobalSettings.SettingsState): Color {
        var color = hexToColorWithAlpha(settings.selectedColor)
        if (settings.useDefaultHighlightColor || color == null) {
            color = UIManager.getColor("List.selectionBackground")
        }
        return color!!
    }

    private fun registerCustomShortcutActions() {
        if (mSettings.openInHorizontalSplit.isNotEmpty()) {
            val action = ShortcutAction("OpenInHorizontalSplit", ShortcutType.OPEN_FILE_IN_HORIZONTAL_SPLIT)
            val tt = KeyStroke.getKeyStroke(mSettings.openInHorizontalSplit)
            if (tt == null) {
                println("Invalid shortcut: ${mSettings.openInHorizontalSplit}")
            }
            val tabShortcut = CustomShortcutSet(KeyStroke.getKeyStroke(mSettings.openInHorizontalSplit))
            action.registerCustomShortcutSet(tabShortcut, mSearchField)
        }
        if (mSettings.openInVerticalSplit.isNotEmpty()) {
            val action = ShortcutAction("OpenInVerticalSplit", ShortcutType.OPEN_FILE_IN_VERTICAL_SPLIT)
            val tabShortcut = CustomShortcutSet(KeyStroke.getKeyStroke(mSettings.openInVerticalSplit))
            action.registerCustomShortcutSet(tabShortcut, mSearchField)
        }
        if (mSettings.openInActiveEditor.isNotEmpty()) {
            val action = ShortcutAction("OpenInActiveEditor", ShortcutType.OPEN_FILE_IN_ACTIVE_EDITOR)
            val tt = KeyStroke.getKeyStroke(mSettings.openInActiveEditor)
            if (tt == null) {
                println("Invalid shortcut: ${mSettings.openInActiveEditor}")
            }
            val tabShortcut = CustomShortcutSet(KeyStroke.getKeyStroke(mSettings.openInActiveEditor))
            action.registerCustomShortcutSet(tabShortcut, mSearchField)
        }
    }

}