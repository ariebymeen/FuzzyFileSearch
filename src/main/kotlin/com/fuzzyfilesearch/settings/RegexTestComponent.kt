package com.fuzzyfilesearch.settings

import com.intellij.ui.components.JBPanel
import java.awt.BorderLayout
import java.awt.GridLayout
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.util.regex.PatternSyntaxException
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class RegexTestComponent : JPanel(BorderLayout()) {
    val mRegexField: JTextField = JTextField()
    val mFilenameField: JTextField = JTextField()
    val mIsValidField: JTextField = JTextField()

    val initialRegexText = "Enter regex"
    val initialFilenameText = "Enter filename to match"

    init {
        val panel = JPanel(GridLayout(1, 3, 10, 0)) // 1 row, 3 columns, 10px gap
        panel.add(mRegexField, BorderLayout.WEST)
        panel.add(mFilenameField, BorderLayout.CENTER)
        panel.add(mIsValidField, BorderLayout.EAST)
        add(panel, BorderLayout.CENTER)

        mRegexField.text = initialRegexText
        mFilenameField.text = initialFilenameText

        mIsValidField.isEditable = false

        mRegexField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) { evaluateRegex() }
            override fun removeUpdate(e: DocumentEvent?) { evaluateRegex() }
            override fun changedUpdate(e: DocumentEvent?) { evaluateRegex() }
        })
        mFilenameField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) { evaluateRegex() }
            override fun removeUpdate(e: DocumentEvent?) { evaluateRegex() }
            override fun changedUpdate(e: DocumentEvent?) { evaluateRegex() }
        })

        mRegexField.addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent) {
                if (mRegexField.text == initialRegexText) mRegexField.text = ""
            }
            override fun focusLost(e: FocusEvent?) {}
        })
        mFilenameField.addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent) {
                if (mFilenameField.text == initialFilenameText) mFilenameField.text = ""
            }
            override fun focusLost(e: FocusEvent?) {}
        })
    }

    fun evaluateRegex() {
        if (mRegexField.text.isEmpty()
            || mRegexField.text == initialRegexText
            || mFilenameField.text.isEmpty()
            || mFilenameField.text == initialFilenameText) {
            return
        }

        try {
            val regex = Regex(pattern = mRegexField.text, options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            if (regex.matches(mFilenameField.text)) {
                mIsValidField.text = "MATCHES"
            } else {
                mIsValidField.text = "DOES NOT MATCH"
            }
        } catch (e: PatternSyntaxException) {
            mIsValidField.text = "INVALID REGEX"
        }

    }
}