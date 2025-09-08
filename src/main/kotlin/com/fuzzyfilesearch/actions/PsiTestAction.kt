package com.fuzzyfilesearch.actions

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.refactoring.rename.RenameUtil
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.project.Project
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.psi.*
import com.intellij.refactoring.rename.RenameHandlerRegistry
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.usageView.UsageInfo
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.psi.util.startOffset
import org.intellij.markdown.IElementType

// TODO: This could open the door for things like:
//  - Search all classes / methods
//  - Search all methods in file
//  - Search all methods in class
//  - Search all methods in namespace

class PsiTestAction : AnAction("My action!"){
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiFile = e.getData(LangDataKeys.PSI_FILE) ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val dataContext = DataManager.getInstance().getDataContext(editor.component)

        //val psiFile = PsiManager.getInstance(project).findFile(virtualFile)

        // Element typically is a PSI element of type IDENTIFIER, with its text the literal text that you see
        // It has a parent element that is the actual expression to be updated changed
        val element = psiFile.findElementAt(editor.caretModel.offset) ?: return

        println("Found element is of type ${element.elementType.toString()}")

        println("Found PSI element with text ${element.text}")
        val snakeCaseText = toSnakeCase(element.text)


        // Resolving the reference works
        val reference = element.parent.reference  // may be null
        val definition: PsiNamedElement? = reference?.resolve() as? PsiNamedElement
        if (definition != null) {
            println("Element is a reference to a named element")
            println("Found definition is of type ${definition.elementType.toString()}")
            RenameProcessor(project, definition, snakeCaseText, true, false).run()
            return
        }

        // This works as expected, but requires manual rename
//        val handler = RenameHandlerRegistry.getInstance().getRenameHandler(dataContext) ?: return
//        handler.invoke(project, editor, psiFile, dataContext)

        // This sometimes works, sometimes not
        // Walk up the tree to find a renamable element
        val namedElement = PsiTreeUtil.getParentOfType(element, PsiNamedElement::class.java)
        if (element is PsiNamedElement) {
            println("Element is a PSI named element")
            RenameProcessor(project, element, snakeCaseText, true, false).run()
        } else if (namedElement != null) {
            println("Element has a parent named element of type named element")
            println("Found named parent element is of type ${namedElement.elementType.toString()}")
            RenameProcessor(project, namedElement, snakeCaseText, true, false).run()
        } else {
            println("PSI element with text ${element.text} is not a named element")
//            val newName = "MyNewName"
//            val processor = RenamePsiElementProcessor.forElement(element)
//            processor.prepareRenaming(element)
//            val scope = GlobalSearchScope.projectScope(project)
//            if (processor.canProcessElement(element)) {
//                println("CAN PROCESS!")
//                val refs = processor.findReferences(element, scope, true)
//                println("Found the following nof refs: ${refs.size}")
//                try {
//                    val usages: Array<UsageInfo> = RenameUtil.findUsages(element, newName, scope,  true, false, emptyMap())
//                    println("Found the following nof usages: ${usages.size}")
//                    usages.forEach { usageInfo ->
//                        val vf = usageInfo.file?.virtualFile
//                        if (vf != null) {
//                            val start = usageInfo.element?.textRange?.startOffset ?: 0
//                            val len = usageInfo.element?.textRange?.length ?: 0
//                            val stop = start + len
//                            println("Found element in file ${vf.name}, from ${start} to ${stop}")
//                        }
//                    }
//                } catch (e: Exception) {
//                    println("Error: ${e.message}")
//                }
//            }
        }
    }

    fun findDeclaration(element: PsiElement): PsiNamedElement? {
        // If it's a reference (usage), resolve it
        val reference = element.reference
        if (reference != null) {
            val resolved = reference.resolve()
            if (resolved is PsiNamedElement) return resolved
        }

        // If the element itself is a declaration
        if (element is PsiNamedElement) return element

        // Fallback: nearest parent in the same file (for things like anonymous classes)
        return PsiTreeUtil.getParentOfType(element, PsiNamedElement::class.java)
    }

    fun toSnakeCase(text: String): String {
        if (text.isEmpty()) return text

        // Step 1: Replace spaces and hyphens with underscores
        val intermediate = text.trim().replace(Regex("[\\s-]+"), "_")

        // Step 2: Convert camelCase or PascalCase to snake_case
        val snake = intermediate.replace(Regex("([a-z0-9])([A-Z])")) {
            "${it.groupValues[1]}_${it.groupValues[2]}"
        }

        // Step 3: Convert to lowercase
        return snake.lowercase()
    }


}