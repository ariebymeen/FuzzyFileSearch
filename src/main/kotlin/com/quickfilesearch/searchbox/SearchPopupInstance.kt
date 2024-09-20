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

//class PopupInstanceItem(val vf: VirtualFile,
//                        var panel: JTextPane? = null
//)
//
//class TransparentTextField(private val opacity: Float) : JTextField() {
//    override fun paintComponent(g: Graphics) {
//        val g2 = g as Graphics2D
//        g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity)
//        super.paintComponent(g2)
//    }
//}

class PopupInstance {
    val searchField: JTextField = JTextField()
    val extensionField: JTextField = TransparentTextField(0.5F)
    val listModel: DefaultListModel<PopupInstanceItem> = DefaultListModel()
    val resultsList = JXList(listModel)
    var onItemSelected: ((PopupInstanceItem) -> Unit)? = null
    var onSearchBoxChanged: ((String) -> List<PopupInstanceItem>)? = null
    var popup: JBPopup? = null
    var maxNofItemsInPopup = 10
    val coroutineScope = CoroutineScope(Dispatchers.Main)
}

// Debounce function
//fun <T> debounce(delayMillis: Long = 30L, coroutineScope: CoroutineScope, action: (T) -> Unit): (T) -> Unit {
//    var debounceJob: Job? = null
//    return { param: T ->
//        debounceJob?.cancel()
//        debounceJob = coroutineScope.launch {
//            delay(delayMillis)
//            action(param)
//        }
//    }
//}

fun updateListedItems(instance: PopupInstance) {
    // TODO: Add indication that not all files are listed
    SwingUtilities.invokeLater {
        val items = instance.onSearchBoxChanged?.invoke(instance.searchField.text);
        items ?: return@invokeLater

//        val start = System.nanoTime()
        val commonCount = kotlin.math.min(items.size, instance.listModel.size())
        for (idx in 0 until commonCount - 1) {
            instance.listModel[idx] = items[idx]
        }

        val itemsToAddCount = if (items.size > commonCount) items.size - commonCount else 0
        if (itemsToAddCount > 0) {
            instance.listModel.addAll(items.slice(commonCount until commonCount + itemsToAddCount))
        }

        val itemsToRemoveCount = if (items.size < instance.listModel.size) instance.listModel.size() - items.size else 0
        if (itemsToRemoveCount > 0) {
            instance.listModel.removeRange(items.size, items.size + itemsToRemoveCount - 1)
        }

//        val stop = System.nanoTime()
//        println("Time spent updating: ${(stop - start) / 1000} us")
        instance.resultsList.selectedIndex = 0
    }
}

fun keyTypedEvent(instance: PopupInstance, e: KeyEvent) {
    if (Character.isDigit(e.keyChar) && e.isControlDown) {
        e.consume() // Consume the event to prevent the character from being added
        instance.resultsList.selectedIndex = e.keyChar.digitToInt()
        instance.popup?.dispose()
        instance.onItemSelected?.invoke(instance.resultsList.selectedValue as PopupInstanceItem)
        return
    }
}

fun keyReleasedEvent(instance: PopupInstance, e: KeyEvent) {
    when (e.keyCode) {
        KeyEvent.VK_UP -> instance.resultsList.selectedIndex -= 1
        KeyEvent.VK_DOWN -> instance.resultsList.selectedIndex += 1
        KeyEvent.VK_TAB -> instance.resultsList.selectedIndex += 1
        KeyEvent.VK_ENTER -> {
            instance.onItemSelected?.invoke(instance.resultsList.selectedValue as PopupInstanceItem)
            instance.popup?.dispose()
        }
    }
    if (e.isControlDown) {
        when (e.keyCode) {
            KeyEvent.VK_K -> instance.resultsList.selectedIndex -= 1
            KeyEvent.VK_J -> instance.resultsList.selectedIndex += 1
        }
    }
}

fun mouseClickedEvent(instance: PopupInstance) {
    instance.popup?.dispose()
    instance.onItemSelected?.invoke(instance.resultsList.selectedValue as PopupInstanceItem)
}

// Create a PopupInstance, register two callbacks:
// getSearchResultCallback: Whenever the search box input changes, this callback is called.
//                          Implement the action you want to do and return a list with items
//                          The selection box will be populated with these items
// itemSelectedCallback   : Whenever one of the results is selected this function is called.
//                          The function is called right after the popup is closed
fun createPopupInstance(
    getSearchResultCallback: ((String) -> List<PopupInstanceItem>),
    itemSelectedCallback: ((PopupInstanceItem) -> Unit),
    settings: GlobalSettings.SettingsState,
    basePath: String,
    project: Project,
    extensions: List<String>? = null
) : PopupInstance
{
    val headerHeight = 40;
    val instance = PopupInstance();
    instance.maxNofItemsInPopup = settings.numberOfFilesInSearchView
    instance.onSearchBoxChanged = getSearchResultCallback
    instance.onItemSelected = itemSelectedCallback

    val border: EmptyBorder = JBUI.Borders.empty(2, 5)
    val panel = JPanel(BorderLayout())
    instance.searchField.preferredSize = Dimension(100, headerHeight)
    instance.searchField.border = border
    instance.searchField.toolTipText = "Type to search..."

    instance.extensionField.border = border
    instance.extensionField.toolTipText = ""
    if (!extensions.isNullOrEmpty()) {
        instance.extensionField.preferredSize = Dimension(100, headerHeight)
        instance.extensionField.text = extensions.map{ext -> ".$ext"}.joinToString(";")
        val width = instance.extensionField.getFontMetrics(instance.extensionField.font).stringWidth(instance.extensionField.text)
        instance.extensionField.preferredSize = Dimension(width + 30, headerHeight)
    } else {
        instance.extensionField.preferredSize = Dimension(0, headerHeight)
    }
    instance.extensionField.isEditable = false

    instance.searchField.addKeyListener(object : KeyAdapter() {
        override fun keyTyped(e: KeyEvent) { keyTypedEvent(instance, e) }
        override fun keyReleased(e: KeyEvent) { keyReleasedEvent(instance, e) }
    })
    val debouncedFunction = debounce<Unit>(50, instance.coroutineScope) { updateListedItems(instance) }
    instance.searchField.document.addDocumentListener(object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent?) { debouncedFunction(Unit) }
        override fun removeUpdate(e: DocumentEvent?) { debouncedFunction(Unit) }
        override fun changedUpdate(e: DocumentEvent?) { debouncedFunction(Unit) }
    })
    instance.resultsList.addListSelectionListener(object : ListSelectionListener {
        override fun valueChanged(e: ListSelectionEvent?) {
            if (e != null) {
//                instance.listModel[instance.resultsList.selectedIndex].rendered = false
                if (!e.valueIsAdjusting) {
                    instance.resultsList.ensureIndexIsVisible(instance.resultsList.selectedIndex)
                }
            }
        }
    })

    // Field with text header
    val searchBar = JPanel(BorderLayout())
    searchBar.add(instance.searchField, BorderLayout.CENTER)
    searchBar.add(instance.extensionField, BorderLayout.EAST)
    panel.add(searchBar, BorderLayout.NORTH)

    instance.resultsList.border = border;
    instance.resultsList.cellRenderer = SearchDialogCellRenderer(settings.filePathDisplayType, basePath, project)
    instance.resultsList.addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent?) { mouseClickedEvent(instance) }
    })
    val scrollPanel = JBScrollPane(instance.resultsList);
    scrollPanel.border = null;
    scrollPanel.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    panel.add(scrollPanel, BorderLayout.CENTER)

    instance.popup = JBPopupFactory.getInstance()
        .createComponentPopupBuilder(panel, panel)
        .setRequestFocus(true)
        .setShowBorder(false)
        .createPopup()

    val start = System.currentTimeMillis()
    updateListedItems(instance)
    val stop = System.currentTimeMillis()
    println("Update listed items took ${stop - start} ms")

    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val width = (screenSize.width * settings.searchPopupWidth).toInt()
    val height = (screenSize.height * settings.searchPopupHeight).toInt()

    instance.popup!!.size = Dimension(width, height)
    val ideFrame = WindowManager.getInstance().getIdeFrame(project)
    if (ideFrame == null) {
        instance.popup!!.showInFocusCenter()
    } else {
        instance.popup!!.showInCenterOf(ideFrame.component)
    }

    SwingUtilities.invokeLater {
        instance.searchField.requestFocusInWindow()
    }

    return instance;
}
