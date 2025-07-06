package com.fuzzyfilesearch.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil


class PsiTestAction : AnAction("My action!"){
    override fun actionPerformed(e: AnActionEvent) {
        val psiFile = e.getData(LangDataKeys.PSI_FILE) ?: return
        val methods = getAllMethodsInFile(psiFile)

        //val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
    }

    fun getAllMethodsInFile(file: PsiFile): List<PsiElement> {
        val methods = mutableListOf<PsiElement>()
//
//        file.accept(object : PsiRecursiveElementVisitor() {
//            override fun visitElement(element: PsiElement) {
//                 Check if the element is a method (works for Java, Kotlin, etc.)
//                if (element is PsiMethod) {
//                    methods.add(element)
//                }
//                super.visitElement(element) // Continue traversal
//            }
//        })
//
        return methods
    }
}