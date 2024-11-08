package com.fuzzyfilesearch.searchbox

import com.fuzzyfilesearch.settings.EditorLocation
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.util.width
import com.intellij.util.ui.JBUI
import com.fuzzyfilesearch.settings.GlobalSettings
import kotlinx.coroutines.*
import org.jdesktop.swingx.JXList
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.*
import kotlin.math.max
import kotlin.math.min

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


class SearchPopupInstance(
    val mGetSearchResultCallback: ((String) -> List<PopupInstanceItem>),
    val mItemSelectedCallback: ((PopupInstanceItem) -> Unit),
    val mSettings: GlobalSettings.SettingsState,
    var mProject: Project,
    var mExtensions: List<String>? = null
) {
    val mSearchField: JTextField = JTextField()
    val mExtensionField: JTextField = TransparentTextField(0.5F) // TODO: Make this editable (grow with input) & allow user to specify this
    val mListModel: DefaultListModel<PopupInstanceItem> = DefaultListModel()
    val mResultsList = JXList(mListModel)
    var mPopup: JBPopup? = null
    var mMaxPopupHeight : Int? = null
    val mCoroutineScope = CoroutineScope(Dispatchers.Main)
    lateinit var mSplitPane: JSplitPane
    val mEditorView = SearchBoxEditor(mProject)
    val mMainPanel: JPanel = JPanel(BorderLayout())
    val mCellRenderer = SearchDialogCellRenderer(mProject, mSettings)

    init {
        createPopupInstance()
    }
    
    fun updatePopupInstance(project: Project, extensions: List<String>?) {
        mProject = project
        mExtensions = extensions

        setSearchBarHeigth(mSettings)
        setExtensionsField(extensions)
        mResultsList.cellRenderer = SearchDialogCellRenderer(mProject, mSettings)
        mSearchField.text = ""

        val splitType = if (mSettings.editorPreviewLocation == EditorLocation.EDITOR_BELOW) JSplitPane.VERTICAL_SPLIT else JSplitPane.HORIZONTAL_SPLIT
        mSplitPane.orientation = splitType
    }

    fun showPopupInstance() {
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        // TODO: Should this be relative to screen size or IDE size?
        val width = (screenSize.width * mSettings.searchPopupWidth).toInt()
        val height = (screenSize.height * mSettings.searchPopupHeight).toInt()
        mMaxPopupHeight = height
        mCellRenderer.maxWidth = width - 20

        val splitPaneSizeAttr = if (mSettings.editorPreviewLocation == EditorLocation.EDITOR_BELOW) height else width
        if (mSettings.showEditorPreview) {
            mSplitPane.dividerLocation = (splitPaneSizeAttr * (1.0 - mSettings.editorSizeRatio)).toInt()
            if (mSettings.editorPreviewLocation == EditorLocation.EDITOR_RIGHT) {
                mCellRenderer.maxWidth = (splitPaneSizeAttr * mSettings.editorSizeRatio).toInt() - 20
            }
        } else {
            mSplitPane.dividerLocation = splitPaneSizeAttr
        }
        mResultsList.cellRenderer = mCellRenderer

        mPopup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(mSplitPane, mSearchField)
            .setRequestFocus(true)
            .setShowBorder(false)
            .setMinSize(Dimension(mSettings.searchBarHeight, 0))
            .createPopup()

        mPopup!!.size = Dimension(width, height)
        val ideFrame = WindowManager.getInstance().getIdeFrame(mProject)
        val ideBounds = WindowManager.getInstance().getFrame(mProject)
        if (ideFrame == null || ideBounds == null) {
            mPopup!!.showInFocusCenter()
        } else {
            val posY = ideBounds.y + ideBounds.height * mSettings.verticalPositionOnScreen - height / 2
            val posX = ideBounds.x + ideBounds.width * mSettings.horizontalPositionOnScreen - width / 2
            val posYInBounds = min(ideBounds.y + ideBounds.height - height, max(ideBounds.y, posY.toInt()))
            val poxXInBounds = max(ideBounds.x, min(ideBounds.x + ideBounds.width - width, posX.toInt()))
            mPopup!!.showInScreenCoordinates(ideFrame.component, Point(poxXInBounds, posYInBounds))
        }

        updateListedItems()
        SwingUtilities.invokeLater {
            mSearchField.requestFocusInWindow()
        }
    }

    private fun updateListedItems() {
        // TODO: Add indication that not all files are listed
        SwingUtilities.invokeLater {
            val items = mGetSearchResultCallback.invoke(mSearchField.text);
            items ?: return@invokeLater

            // update list items. This is optimized for performance as clearing the list model gives problems
            val commonCount = kotlin.math.min(items.size, mListModel.size())
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
            if (mSettings.showEditorPreview && mResultsList.selectedIndex >= 0) {
                val selectedFile = mResultsList.selectedValue as PopupInstanceItem
                mEditorView.updateFile(selectedFile.vf)
            }

            if (mSettings.shrinkViewDynamically) {
                mPopup!!.size = Dimension(mPopup!!.width, min(mMaxPopupHeight!!, mSettings.searchBarHeight + mListModel.size() * mSettings.searchItemHeight + 4))
                mResultsList.revalidate()
                mResultsList.repaint()
            }
        }
    }

    fun keyTypedEvent(e: KeyEvent) {
        if (Character.isDigit(e.keyChar) && e.isControlDown) {
            e.consume() // Consume the event to prevent the character from being added
            mResultsList.selectedIndex = e.keyChar.digitToInt()
            mPopup?.dispose()
            mItemSelectedCallback.invoke(mResultsList.selectedValue as PopupInstanceItem)
            return
        }
    }

    fun keyReleasedEvent(e: KeyEvent) {
        when (e.keyCode) {
            KeyEvent.VK_UP -> mResultsList.selectedIndex -= 1
            KeyEvent.VK_DOWN -> mResultsList.selectedIndex += 1
            KeyEvent.VK_TAB -> mResultsList.selectedIndex += 1
            KeyEvent.VK_ENTER -> {
                println("Pressed enter!")
                if (mResultsList.selectedIndex >= 0) {
                    mItemSelectedCallback.invoke(mResultsList.selectedValue as PopupInstanceItem)
                    mPopup?.dispose()
                }
            }
        }
        if (e.isControlDown) {
            when (e.keyCode) {
                KeyEvent.VK_K -> mResultsList.selectedIndex -= 1
                KeyEvent.VK_J -> mResultsList.selectedIndex += 1
            }
        }
    }

    fun mouseClickedEvent() {
        mPopup?.dispose()
        mItemSelectedCallback.invoke(mResultsList.selectedValue as PopupInstanceItem)
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

    private fun createPopupInstance() {
        val border: EmptyBorder = JBUI.Borders.empty(2, 5)
        mSearchField.border = border
        mSearchField.toolTipText = "Type to search..."

        mExtensionField.border = border
        mExtensionField.toolTipText = ""
        mExtensionField.isEditable = false
        setExtensionsField(mExtensions)
        setSearchBarHeigth(mSettings)

        mSearchField.addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent) { keyTypedEvent(e) }
            override fun keyReleased(e: KeyEvent) { keyReleasedEvent(e) }
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
                    if (mSettings.showEditorPreview && mResultsList.selectedIndex >= 0) {
                        val selectedFile = mResultsList.selectedValue as PopupInstanceItem
                        mEditorView.updateFile(selectedFile.vf)
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
        mResultsList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) { mouseClickedEvent() }
        })
        val scrollPanel = JBScrollPane(mResultsList);
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
            .setMinSize(Dimension(mSettings.searchBarHeight, 0))
            .createPopup()

    }

}