package com.fuzzyfilesearch.settings

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.*
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileWrapper
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.ui.util.preferredHeight
import java.awt.BorderLayout
import java.awt.event.ComponentAdapter
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.IOException
import javax.swing.*
import javax.swing.table.DefaultTableModel

class ActionsTable(columnNames: Array<String>,
                   private val emptyItem: Array<String>,
                   private val weights: Array<Int>? = null) : JBPanel<ActionsTable>() {

                       // TODO: Resize according to content
                       // TODO: Double click for flyout that is editable
                       // TODO: Single click to edit contents
    private val mTableModel = DefaultTableModel(columnNames, 0)
    private val mTable = JBTable(mTableModel)
    private val mTablePanel = JBPanel<JBPanel<*>>()
    private val rowHeight = 30
    private var mColumnWeights = Array(columnNames.size) { 1 }

    init {
        if (weights != null && weights.size == columnNames.size) {
            mColumnWeights = weights
        }

        layout = BorderLayout()

        // Table panel
        mTableModel.isCellEditable(0, 0)
        mTable.rowHeight = rowHeight
        mTable.autoResizeMode = JTable.AUTO_RESIZE_OFF
        mTable.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                val row = mTable.rowAtPoint(e.point)
                val column = mTable.columnAtPoint(e.point)

                if (row != -1 && column != -1 && mTable.isCellEditable(row, column)) {
                    mTable.editCellAt(row, column)
                    val editorComponent = mTable.editorComponent
                    editorComponent?.requestFocus()
                }
            }
        })
        // Listen for table resize events
        mTablePanel.addComponentListener(object: ComponentAdapter() {
            override fun componentResized(e: java.awt.event.ComponentEvent) {
                resizeColumns()
            }
        })
        mTablePanel.apply {
            layout = BorderLayout()
            val scrollTable = JBScrollPane(mTable)
            for (listener in scrollTable.mouseWheelListeners) {
                // Remove existing MouseWheelListeners to ensure scrolling still works nicely
                scrollTable.removeMouseWheelListener(listener)
            }

            add(scrollTable, BorderLayout.CENTER)
        }
        mTablePanel.preferredHeight = rowHeight * 5
        add(mTablePanel, BorderLayout.CENTER)

        val formPanel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            val addButton = JButton("New").apply {
                addActionListener {
                    mTableModel.addRow(emptyItem)
                    resize()
                }
            }
            add(addButton)
            val removeButton = JButton("Delete").apply {
                addActionListener {
                    if (mTable.selectedRow >= 0 && !mTable.isEditing) {
                        mTableModel.removeRow(mTable.selectedRow)
                        resize()
                    }
                }
            }
            add(removeButton)
            val duplicateButton = JButton("Duplicate").apply {
                addActionListener {
                    if (mTable.selectedRow >= 0) {
                        val row = getRowEntry(mTable.selectedRow) ?: return@addActionListener
                        mTableModel.addRow(row)
                        resize()
                    }
                }
            }
            add(duplicateButton)
            val exportButton = JButton("Export").apply {
                addActionListener {
                    exportToFile()
                }
            }
            add(exportButton)
            val importButton = JButton("Import").apply {
                addActionListener {
                    importFromFile()
                }
            }
            add(importButton)
        }
        add(formPanel, BorderLayout.SOUTH)
    }

    fun resize() {
        mTablePanel.preferredHeight = rowHeight * kotlin.math.max(5, kotlin.math.min(mTable.rowCount + 1, 10))
        mTablePanel.revalidate()
        mTablePanel.repaint()
    }

    fun getRowEntry(rowIndex: Int) : Array<String>? {
        if (rowIndex < 0 || rowIndex >= mTableModel.rowCount) {
            return null
        }

        var row = emptyArray<String>()
        for (i in 0..mTableModel.columnCount - 1) {
            row += mTableModel.getValueAt(rowIndex, i) as String;
        }
        return row
    }

    fun getData() : Array<Array<String>> {
        var data: Array<Array<String>> = emptyArray()
        for (row in 0..mTableModel.rowCount-1) {
            data += getRowEntry(row)!!
        }
        return data
    }

    fun setData(data: Array<Array<String>>) {
        mTableModel.rowCount = 0
        for (row in data) {
            if (mTableModel.columnCount != row.size) {
                println("Data is not of the correct size, columnCount: ${mTableModel.columnCount}, data size: ${row.size}")
                break
            }

            mTableModel.addRow(row)
        }
        resize()
    }

    fun resizeColumns() {
        val totalWeight = mColumnWeights.sum().toDouble()
        val tableWidth = mTable.parent.width
        for (i in 0 until mTable.columnCount) {
            val column = mTable.columnModel.getColumn(i)
            val weightRatio = mColumnWeights[i].toDouble() / totalWeight
            val newWidth = (tableWidth * weightRatio).toInt()
            column.preferredWidth = newWidth
            column.width = newWidth
        }
        mTablePanel.revalidate()
        mTablePanel.repaint()
    }

    fun exportToFile() {
        val gson: Gson = Gson()
        val jsonData: String = gson.toJson(getData())

        // Open a file chooser where the user can specify the file name
        val descriptor = FileSaverDescriptor("Save JSON File", "Choose a location to save")
        val dialog: FileSaverDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, null)
        val fileWrapper: VirtualFileWrapper? = dialog.save(null as VirtualFile?, "exported_data.json")

        fileWrapper?.file?.let { file ->
            ApplicationManager.getApplication().runWriteAction {
                try {
                    file.writeText(jsonData)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun importFromFile() {
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
        val file = FileChooser.chooseFile(descriptor, null, null) ?: return

        try {
            val content = VfsUtil.loadText(file)
            val gson = Gson()
            val listType = object : TypeToken<Array<Array<String>>>() {}.type
            setData(gson.fromJson(content, listType))
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return
    }
}