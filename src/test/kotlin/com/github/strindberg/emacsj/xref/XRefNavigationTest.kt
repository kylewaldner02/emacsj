package com.github.strindberg.emacsj.xref

import com.intellij.openapi.actionSystem.IdeActions.ACTION_GOTO_IMPLEMENTATION
import com.intellij.testFramework.fixtures.BasePlatformTestCase

private const val FILE = "X.java"

private val TEXT =
    """
        interface X {
            void foo();
        }

        class Y implements X {
            public void foo() {
                System.out.println("Hello world!");
            }
        }

        class A {
            void bar(X x) {
                x.foo();
            }
        }
    """.trimIndent()

class XRefNavigationTest : BasePlatformTestCase() {

    fun `test navigation to an implementation pushes the place navigated away from`() {
        myFixture.configureByText(FILE, TEXT)

        val origin = TEXT.indexOf("foo();", TEXT.indexOf("void bar")) + 2
        myFixture.editor.caretModel.moveToOffset(origin)

        myFixture.performEditorAction(ACTION_GOTO_IMPLEMENTATION)
        assertTrue("Navigation did not happen", myFixture.editor.caretModel.offset != origin)

        myFixture.performEditorAction(ACTION_XREF_BACK)

        assertEquals(origin, myFixture.editor.caretModel.offset)
    }
}
