package com.fuzzyfilesearch.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

class PrintPsiElementTypes() : AnAction(utils.getActionName("PrintPsiElementTypes")) {
    var mElementTypes: MutableMap<String, Int> = mutableMapOf()
    override fun actionPerformed(e: AnActionEvent) {
        val psiFile = e.getData(LangDataKeys.PSI_FILE) ?: return
        val file    = e.getData(CommonDataKeys.EDITOR)?.virtualFile ?: return

        countCodeTypeElements(psiFile)

        println("Found the following code type elements in ${file.name}:")
        mElementTypes.forEach { println("${it.key} (Found ${it.value} times)")}
    }

    private fun countCodeTypeElements(element: PsiElement) {
        if (mElementTypes.get(element.elementType.toString()) != null) {
            mElementTypes[element.elementType.toString()] = mElementTypes[element.elementType.toString()]!! + 1
        } else {
            mElementTypes[element.elementType.toString()] = 1
        }
        for (child in element.children) {
            countCodeTypeElements(child)
        }
    }
}