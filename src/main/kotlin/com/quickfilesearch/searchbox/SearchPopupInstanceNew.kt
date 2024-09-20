package com.quickfilesearch.searchbox

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.quickfilesearch.settings.GlobalSettings
import kotlinx.coroutines.*
import org.jdesktop.swingx.JXList
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.*

class PopupInstanceItem(val vf: VirtualFile,
                        var panel: JTextPane? = null
)

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
    val getSearchResultCallback: ((String) -> List<PopupInstanceItem>),
    val itemSelectedCallback: ((PopupInstanceItem) -> Unit),
    val settings: GlobalSettings.SettingsState,
    val basePath: String,
    val project: Project,
    val extensions: List<String>? = null
) {
    val searchField: JTextField = JTextField()
    val extensionField: JTextField = TransparentTextField(0.5F)
    val listModel: DefaultListModel<PopupInstanceItem> = DefaultListModel()
    val resultsList = JXList(listModel)
    var popup: JBPopup? = null
    var maxNofItemsInPopup = 10
    val headerHeight = 30
    val coroutineScope = CoroutineScope(Dispatchers.Main)

    init {
        createPopupInstance()
    }

    fun showPopupInstance() {
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        val width = (screenSize.width * settings.searchPopupWidth).toInt()
        val height = (screenSize.height * settings.searchPopupHeight).toInt()

        popup!!.size = Dimension(width, height)
        val ideFrame = WindowManager.getInstance().getIdeFrame(project)
        if (ideFrame == null) {
            popup!!.showInFocusCenter()
        } else {
            popup!!.showInCenterOf(ideFrame.component)
        }

        updateListedItems()
        SwingUtilities.invokeLater {
            searchField.requestFocusInWindow()
        }
    }

    private fun updateListedItems() {
        // TODO: Add indication that not all files are listed
        SwingUtilities.invokeLater {
            val items = getSearchResultCallback.invoke(searchField.text);
            items ?: return@invokeLater

            val commonCount = kotlin.math.min(items.size, listModel.size())
            for (idx in 0 until commonCount - 1) {
                listModel[idx] = items[idx]
            }

            val itemsToAddCount = if (items.size > commonCount) items.size - commonCount else 0
            if (itemsToAddCount > 0) {
                listModel.addAll(items.slice(commonCount until commonCount + itemsToAddCount))
            }

            val itemsToRemoveCount = if (items.size < listModel.size) listModel.size() - items.size else 0
            if (itemsToRemoveCount > 0) {
                listModel.removeRange(items.size, items.size + itemsToRemoveCount - 1)
            }

            resultsList.selectedIndex = 0
        }
    }

    fun keyTypedEvent(e: KeyEvent) {
        if (Character.isDigit(e.keyChar) && e.isControlDown) {
            e.consume() // Consume the event to prevent the character from being added
            resultsList.selectedIndex = e.keyChar.digitToInt()
            popup?.dispose()
            itemSelectedCallback.invoke(resultsList.selectedValue as PopupInstanceItem)
            return
        }
    }

    fun keyReleasedEvent(e: KeyEvent) {
        when (e.keyCode) {
            KeyEvent.VK_UP -> resultsList.selectedIndex -= 1
            KeyEvent.VK_DOWN -> resultsList.selectedIndex += 1
            KeyEvent.VK_TAB -> resultsList.selectedIndex += 1
            KeyEvent.VK_ENTER -> {
                itemSelectedCallback.invoke(resultsList.selectedValue as PopupInstanceItem)
                popup?.dispose()
            }
        }
        if (e.isControlDown) {
            when (e.keyCode) {
                KeyEvent.VK_K -> resultsList.selectedIndex -= 1
                KeyEvent.VK_J -> resultsList.selectedIndex += 1
            }
        }
    }

    fun mouseClickedEvent() {
        popup?.dispose()
        itemSelectedCallback.invoke(resultsList.selectedValue as PopupInstanceItem)
    }

    private fun createPopupInstance() {
        maxNofItemsInPopup = settings.numberOfFilesInSearchView

        val border: EmptyBorder = JBUI.Borders.empty(2, 5)
        val panel = JPanel(BorderLayout())
        searchField.preferredSize = Dimension(0, headerHeight)
        searchField.border = border
        searchField.toolTipText = "Type to search..."

        extensionField.border = border
        extensionField.toolTipText = ""
        if (!extensions.isNullOrEmpty()) {
//            extensionField.preferredSize = Dimension(100, headerHeight)
            extensionField.text = extensions.map{ext -> ".$ext"}.joinToString(";")
            val width = extensionField.getFontMetrics(extensionField.font).stringWidth(extensionField.text)
            extensionField.preferredSize = Dimension(width + 30, headerHeight)
        } else {
            extensionField.preferredSize = Dimension(0, headerHeight)
        }
        extensionField.isEditable = false

        searchField.addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent) { keyTypedEvent(e) }
            override fun keyReleased(e: KeyEvent) { keyReleasedEvent(e) }
        })
        val debouncedFunction = debounce<Unit>(50, coroutineScope) { updateListedItems() }
        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) { debouncedFunction(Unit) }
            override fun removeUpdate(e: DocumentEvent?) { debouncedFunction(Unit) }
            override fun changedUpdate(e: DocumentEvent?) { debouncedFunction(Unit) }
        })
        resultsList.addListSelectionListener(object : ListSelectionListener {
            override fun valueChanged(e: ListSelectionEvent?) {
                if (e != null && !e.valueIsAdjusting) {
                    resultsList.ensureIndexIsVisible(resultsList.selectedIndex)
                }
            }
        })

        // Field with text header
        val searchBar = JPanel(BorderLayout())
        searchBar.add(searchField, BorderLayout.CENTER)
        searchBar.add(extensionField, BorderLayout.EAST)
        panel.add(searchBar, BorderLayout.NORTH)

//        resultsList.border = border
        resultsList.border = null
        resultsList.cellRenderer = SearchDialogCellRenderer(settings.filePathDisplayType, basePath, project)
        resultsList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) { mouseClickedEvent() }
        })
        val scrollPanel = JBScrollPane(resultsList);
        scrollPanel.border = null;
        scrollPanel.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        panel.add(scrollPanel, BorderLayout.CENTER)

        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, panel)
            .setRequestFocus(true)
            .setShowBorder(false)
            .createPopup()
    }
}




// Create a PopupInstance, register two callbacks:
// getSearchResultCallback: Whenever the search box input changes, this callback is called.
//                          Implement the action you want to do and return a list with items
//                          The selection box will be populated with these items
// itemSelectedCallback   : Whenever one of the results is selected this function is called.
//                          The function is called right after the popup is closed
//fun createPopupInstance(
//    getSearchResultCallback: ((String) -> List<PopupInstanceItem>),
//    itemSelectedCallback: ((PopupInstanceItem) -> Unit),
//    settings: GlobalSettings.SettingsState,
//    basePath: String,
//    project: Project,
//    extensions: List<String>? = null
//) : PopupInstance
//{
//
//    val start = System.currentTimeMillis()
//    updateListedItems(instance)
//    val stop = System.currentTimeMillis()
//    println("Update listed items took ${stop - start} ms")
//
//    val screenSize = Toolkit.getDefaultToolkit().screenSize
//    val width = (screenSize.width * settings.searchPopupWidth).toInt()
//    val height = (screenSize.height * settings.searchPopupHeight).toInt()
//
//    instance.popup!!.size = Dimension(width, height)
//    val ideFrame = WindowManager.getInstance().getIdeFrame(project)
//    if (ideFrame == null) {
//        instance.popup!!.showInFocusCenter()
//    } else {
//        instance.popup!!.showInCenterOf(ideFrame.component)
//    }
//
//    SwingUtilities.invokeLater {
//        instance.searchField.requestFocusInWindow()
//    }
//
//    return instance;
//}
