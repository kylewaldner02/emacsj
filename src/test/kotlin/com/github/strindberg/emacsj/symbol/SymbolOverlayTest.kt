package com.github.strindberg.emacsj.symbol

import com.github.strindberg.emacsj.actions.symbol.ACTION_SYMBOL_OVERLAY_JUMP_NEXT
import com.github.strindberg.emacsj.actions.symbol.ACTION_SYMBOL_OVERLAY_JUMP_PREV
import com.intellij.testFramework.fixtures.BasePlatformTestCase

private const val FILE = "MyClass.kt"

private val TEXT =
    """
        class MyClass {
            val downloadExportUseCaseTest = 1
            val downloadExportUseCase = 2
            val mydownloadExportUseCase = 3

            fun run() {
                downloadExportUseCase.execute()
            }
        }
    """.trimIndent()

private val FIRST = TEXT.indexOf("downloadExportUseCase = 2")
private val SECOND = TEXT.indexOf("downloadExportUseCase.execute()")

class SymbolOverlayTest : BasePlatformTestCase() {

    fun `test jump to next occurrence skips words that only contain the symbol`() {
        myFixture.configureByText(FILE, TEXT)
        myFixture.editor.caretModel.moveToOffset(FIRST)

        myFixture.performEditorAction(ACTION_SYMBOL_OVERLAY_JUMP_NEXT)

        assertEquals(SECOND, myFixture.editor.caretModel.offset)
    }

    fun `test jump to previous occurrence skips words that only contain the symbol`() {
        myFixture.configureByText(FILE, TEXT)
        myFixture.editor.caretModel.moveToOffset(SECOND)

        myFixture.performEditorAction(ACTION_SYMBOL_OVERLAY_JUMP_PREV)

        assertEquals(FIRST, myFixture.editor.caretModel.offset)
    }

    fun `test jump to next occurrence wraps around the file`() {
        myFixture.configureByText(FILE, TEXT)
        myFixture.editor.caretModel.moveToOffset(SECOND)

        myFixture.performEditorAction(ACTION_SYMBOL_OVERLAY_JUMP_NEXT)

        assertEquals(FIRST, myFixture.editor.caretModel.offset)
    }

    fun `test jump to previous occurrence wraps around the file`() {
        myFixture.configureByText(FILE, TEXT)
        myFixture.editor.caretModel.moveToOffset(FIRST)

        myFixture.performEditorAction(ACTION_SYMBOL_OVERLAY_JUMP_PREV)

        assertEquals(SECOND, myFixture.editor.caretModel.offset)
    }

    fun `test caret keeps its position within the symbol when jumping`() {
        myFixture.configureByText(FILE, TEXT)
        myFixture.editor.caretModel.moveToOffset(FIRST + "download".length)

        myFixture.performEditorAction(ACTION_SYMBOL_OVERLAY_JUMP_NEXT)

        assertEquals(SECOND + "download".length, myFixture.editor.caretModel.offset)
    }

    fun `test jumping from a word that contains the symbol stays among that word's occurrences`() {
        myFixture.configureByText(FILE, TEXT)
        val decoy = TEXT.indexOf("downloadExportUseCaseTest")
        myFixture.editor.caretModel.moveToOffset(decoy)

        myFixture.performEditorAction(ACTION_SYMBOL_OVERLAY_JUMP_NEXT)

        assertEquals(decoy, myFixture.editor.caretModel.offset)
    }
}
