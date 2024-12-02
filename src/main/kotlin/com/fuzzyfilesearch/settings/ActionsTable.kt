package com.fuzzyfilesearch.settings

import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.ui.util.preferredHeight
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.table.DefaultTableModel

class ActionsTable(columnNames: Array<String>,
                   private val emptyItem: Array<String>) : JBPanel<ActionsTable>() {

    private val mTableModel = DefaultTableModel(columnNames, 0)
    private val mTable = JBTable(mTableModel)
    private val mTablePanel = JBPanel<JBPanel<*>>()
    private val rowHeight = 30

    init {
        layout = BorderLayout()

        // Table panel
        mTableModel.isCellEditable(0, 0)
        mTable.rowHeight = rowHeight
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
            val addButton = JButton("Add new action").apply {
                addActionListener {
                    mTableModel.addRow(emptyItem)
                    resize()
                }
            }
            add(addButton)
            val removeButton = JButton("Remove action").apply {
                addActionListener {
                    if (mTable.selectedRow >= 0 && !mTable.isEditing) {
                        mTableModel.removeRow(mTable.selectedRow)
                        resize()
                    }
                }
            }
            add(removeButton)
            val duplicateButton = JButton("Duplicate action").apply {
                addActionListener {
                    if (mTable.selectedRow >= 0) {
                        val row = getRowEntry(mTable.selectedRow) ?: return@addActionListener
                        mTableModel.addRow(row)
                        resize()
                    }
                }
            }
            add(duplicateButton)
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
}