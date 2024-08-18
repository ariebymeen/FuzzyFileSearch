package com.quickfilesearch.settings

import com.intellij.ui.components.JBPanel
import com.intellij.ui.util.preferredHeight
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

class ActionsTable(columnNames: Array<String>,
                   private val emptyItem: Array<String>) : JBPanel<ActionsTable>() {

    private val tableModel = DefaultTableModel(columnNames, 0)

    init {
        layout = BorderLayout()

        // Table panel
        tableModel.isCellEditable(0, 0)
        val table = JTable(tableModel)
        table.rowHeight = 30 // Set each row's height to 30 pixels
        val tablePanel = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()
            add(JScrollPane(table), BorderLayout.CENTER)
        }
        tablePanel.preferredHeight = 30 * 5

        add(tablePanel, BorderLayout.CENTER)

        val formPanel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            val addButton = JButton("Add new action").apply {
                addActionListener {
                    tableModel.addRow(emptyItem)
                }
            }
            add(addButton)
            val removeButton = JButton("Remove action").apply {
                addActionListener {
                    if (table.selectedRow >= 0) {
                        tableModel.removeRow(table.selectedRow)
                    }
                }
            }
            add(removeButton)
            val duplicateButton = JButton("Duplicate action").apply {
                addActionListener {
                    if (table.selectedRow >= 0) {
                        val row = getRowEntry(table.selectedRow) ?: return@addActionListener
                        tableModel.addRow(row)
                    }
                }
            }
            add(duplicateButton)
        }
        add(formPanel, BorderLayout.SOUTH)
    }

    fun getRowEntry(rowIndex: Int) : Array<String>? {
        if (rowIndex < 0 || rowIndex >= tableModel.rowCount) {
            return null
        }

        var row = emptyArray<String>()
        for (i in 0..tableModel.columnCount - 1) {
            row += tableModel.getValueAt(rowIndex, i) as String;
        }
        return row
    }

    fun getData() : Array<Array<String>> {
        var data: Array<Array<String>> = emptyArray()
        for (row in 0..tableModel.rowCount-1) {
            data += getRowEntry(row)!!
        }
        return data
    }

    fun setData(data: Array<Array<String>>) {
        tableModel.rowCount = 0
        for (row in data) {
            tableModel.addRow(row)
        }
    }
}