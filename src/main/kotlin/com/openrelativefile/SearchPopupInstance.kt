package com.openrelativefile

import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import com.openrelativefile.Settings.GlobalSettings
import kotlinx.coroutines.*

class PopupInstance {
    val searchField: JTextField = JTextField()
    // TODO: Use the VirtualFile as the list model. Provide a callback to convert to string. This way, we can render with or without path
//    val listModel: DefaultListModel<String> = DefaultListModel()
    val listModel: DefaultListModel<VirtualFile> = DefaultListModel()
    val resultsList = JBList(listModel)
    var onItemSelected: ((VirtualFile) -> Unit)? = null
    var onSearchBoxChanged: ((String) -> List<VirtualFile>)? = null
    var popup: JBPopup? = null
    var maxNofItemsInPopup = 10
    val coroutineScope = CoroutineScope(Dispatchers.Main)
}

// Debounce function
fun <T> debounce(delayMillis: Long = 300L, coroutineScope: CoroutineScope, action: (T) -> Unit): (T) -> Unit {
    var debounceJob: Job? = null
    return { param: T ->
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(delayMillis)
            action(param)
        }
    }
}

fun updateListedItems(instance: PopupInstance) {
    val items = instance.onSearchBoxChanged?.invoke(instance.searchField.text);
    instance.listModel.clear()
    for (item in items!!) {
        if (instance.listModel.size() >= instance.maxNofItemsInPopup) break
        instance.listModel.addElement(item)
    }
    instance.resultsList.selectedIndex = 0
//    instance.resultsList.ensureIndexIsVisible(0)
}

fun keyTypedEvent(instance: PopupInstance, e: KeyEvent) {
    if (Character.isDigit(e.keyChar) && (instance.searchField.text.isEmpty() || e.isControlDown)) {
        e.consume() // Consume the event to prevent the character from being added
        // TODO: Add VIM integration
        instance.resultsList.selectedIndex = e.keyChar.digitToInt()
        instance.popup?.dispose()
        instance.onItemSelected?.invoke(instance.resultsList.selectedValue)
        return
    }
}

fun keyReleasedEvent(instance: PopupInstance, e: KeyEvent) {
    when (e.keyCode) {
        KeyEvent.VK_UP -> instance.resultsList.selectedIndex -= 1
        KeyEvent.VK_DOWN -> instance.resultsList.selectedIndex += 1
        KeyEvent.VK_ENTER -> {
            instance.onItemSelected?.invoke(instance.resultsList.selectedValue)
            instance.popup?.dispose()
        }
    }
    if (e.isControlDown) {
        when (e.keyChar) {
            'k' -> instance.resultsList.selectedIndex -= 1
            'j' -> instance.resultsList.selectedIndex += 1
        }
    }
}

fun mouseClickedEvent(instance: PopupInstance) {
    instance.popup?.dispose()
    instance.onItemSelected?.invoke(instance.resultsList.selectedValue)
}

// Create a PopupInstance, register two callbacks:
// getSearchResultCallback: Whenever the search box input changes, this callback is called.
//                          Implement the action you want to do and return a list with items
//                          The selection box will be populated with these items
// itemSelectedCallback   : Whenever one of the results is selected this function is called.
//                          The function is called right after the popup is closed
fun createPopupInstance(
    getSearchResultCallback: ((String) -> List<VirtualFile>),
    itemSelectedCallback: ((VirtualFile) -> Unit),
    settings: GlobalSettings.SettingsState,
    basePath: String
) : PopupInstance
{

    val instance = PopupInstance();
    instance.maxNofItemsInPopup = settings.numberOfFilesInSearchView
    instance.onSearchBoxChanged = getSearchResultCallback;
    instance.onItemSelected = itemSelectedCallback;

    val border: EmptyBorder = JBUI.Borders.empty(2, 5);
    val panel = JPanel(BorderLayout());
    instance.searchField.preferredSize = Dimension(100, 50);
    instance.searchField.border = border;
    instance.searchField.toolTipText = "Type to search..."

    instance.searchField.addKeyListener(object : KeyAdapter() {
        override fun keyTyped(e: KeyEvent) {
            keyTypedEvent(instance, e)
        }
        override fun keyReleased(e: KeyEvent) { keyReleasedEvent(instance, e) }
    })
    val debouncedFunction = debounce<Unit>(50, instance.coroutineScope) { updateListedItems(instance) }
    instance.searchField.document.addDocumentListener(object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent?) { debouncedFunction(Unit) }
        override fun removeUpdate(e: DocumentEvent?) { debouncedFunction(Unit) }
        override fun changedUpdate(e: DocumentEvent?) { debouncedFunction(Unit) }
    })
    panel.add(instance.searchField, BorderLayout.NORTH)

    instance.resultsList.border = border;
    instance.resultsList.cellRenderer = SearchDialogCellRenderer(settings.filePathDisplayType, basePath)
    instance.resultsList.addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent?) { mouseClickedEvent(instance) }
    })
    val scrollPanel = JBScrollPane(instance.resultsList);
    scrollPanel.border = null;
    panel.add(scrollPanel, BorderLayout.CENTER)

    instance.popup = JBPopupFactory.getInstance()
        .createComponentPopupBuilder(panel, panel)
        .setRequestFocus(true)
        .setShowBorder(false)
        .createPopup()

    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val width = (screenSize.width * settings.searchPopupWidth).toInt()
    val height = (screenSize.height * settings.searchPopupHeight).toInt()

    instance.popup!!.setSize(Dimension(width, height))
    instance.popup!!.showInFocusCenter()

    // Set focus to searchField
    SwingUtilities.invokeLater { instance.searchField.requestFocusInWindow() }

    // Initial population of the search list
    updateListedItems(instance);

    return instance;
}
