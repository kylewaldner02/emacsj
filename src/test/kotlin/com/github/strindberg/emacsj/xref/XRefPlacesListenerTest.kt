package com.github.strindberg.emacsj.xref

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl.RecentPlacesListener
import com.intellij.platform.backend.navigation.NavigationRequest
import com.intellij.platform.backend.navigation.NavigationRequests
import com.intellij.platform.ide.navigation.NavigationOptions
import com.intellij.testFramework.fixtures.BasePlatformTestCase

private const val FILE = "MyClass.kt"

private val TEXT =
    """
        class MyClass {
            fun main() {
                hello()
            }
            fun hello() {
                println("Hello world!")
            }
        }
    """.trimIndent()

class XRefPlacesListenerTest : BasePlatformTestCase() {

    fun `test place left by navigation to a position is pushed onto the xref stack`() {
        myFixture.configureByText(FILE, TEXT)

        val origin = moveTo(TEXT.indexOf("hello()"))
        val destination = navigateTo(TEXT.indexOf("println"), from = origin)
        assertEquals(destination.offset, myFixture.editor.caretModel.offset)

        myFixture.performEditorAction(ACTION_XREF_BACK)

        assertEquals(origin.offset, myFixture.editor.caretModel.offset)
    }

    fun `test place left by navigation to a position can be redone`() {
        myFixture.configureByText(FILE, TEXT)

        val origin = moveTo(TEXT.indexOf("hello()"))
        val destination = navigateTo(TEXT.indexOf("println"), from = origin)

        myFixture.performEditorAction(ACTION_XREF_BACK)
        myFixture.performEditorAction(ACTION_XREF_FORWARD)

        assertEquals(destination.offset, myFixture.editor.caretModel.offset)
    }

    fun `test place left by navigation without a position is not pushed onto the xref stack`() {
        myFixture.configureByText(FILE, TEXT)

        val origin = moveTo(TEXT.indexOf("hello()"))
        announceNavigation(offset = null)
        recordPlace(origin)
        val destination = moveTo(TEXT.indexOf("println"))

        myFixture.performEditorAction(ACTION_XREF_BACK)

        assertEquals(destination.offset, myFixture.editor.caretModel.offset)
    }

    fun `test place recorded without preceding navigation is not pushed onto the xref stack`() {
        myFixture.configureByText(FILE, TEXT)

        val origin = moveTo(TEXT.indexOf("hello()"))
        recordPlace(origin)
        val destination = moveTo(TEXT.indexOf("println"))

        myFixture.performEditorAction(ACTION_XREF_BACK)

        assertEquals(destination.offset, myFixture.editor.caretModel.offset)
    }

    fun `test changed place is not pushed onto the xref stack`() {
        myFixture.configureByText(FILE, TEXT)

        val origin = moveTo(TEXT.indexOf("hello()"))
        announceNavigation(TEXT.indexOf("println"))
        recordPlace(origin, isChanged = true)
        val destination = moveTo(TEXT.indexOf("println"))

        myFixture.performEditorAction(ACTION_XREF_BACK)

        assertEquals(destination.offset, myFixture.editor.caretModel.offset)
    }

    fun `test only one place is pushed for a single navigation`() {
        myFixture.configureByText(FILE, TEXT)

        val first = moveTo(TEXT.indexOf("class"))
        val second = navigateTo(TEXT.indexOf("hello()"), from = first)
        recordPlace(second)
        val third = navigateTo(TEXT.indexOf("println"), from = second)
        assertEquals(third.offset, myFixture.editor.caretModel.offset)

        myFixture.performEditorAction(ACTION_XREF_BACK)
        assertEquals(second.offset, myFixture.editor.caretModel.offset)

        myFixture.performEditorAction(ACTION_XREF_BACK)
        assertEquals(first.offset, myFixture.editor.caretModel.offset)
    }

    private fun navigateTo(offset: Int, from: Place): Place {
        announceNavigation(offset)
        recordPlace(from)
        return moveTo(offset)
    }

    private fun announceNavigation(offset: Int?) {
        XRefNavigationHandler().navigate(
            navigationRequest(offset ?: -1),
            NavigationOptions.defaultOptions(),
            DataContext.EMPTY_CONTEXT,
        )
    }

    /**
     * A request is computed the way the platform computes it: in a read action on a background thread. An offset below
     * zero is what navigation that only opens a file, without a position in it, is made up of.
     */
    private fun navigationRequest(offset: Int): NavigationRequest =
        checkNotNull(
            ApplicationManager.getApplication().executeOnPooledThread<NavigationRequest?> {
                runReadAction {
                    NavigationRequests.getInstance()
                        .sourceNavigationRequest(myFixture.project, myFixture.file.virtualFile, offset, null)
                }
            }.get()
        )

    private fun moveTo(offset: Int): Place {
        myFixture.editor.caretModel.moveToOffset(offset)
        return Place(offset, currentPlace())
    }

    private fun recordPlace(place: Place, isChanged: Boolean = false) {
        myFixture.project.messageBus.syncPublisher(RecentPlacesListener.TOPIC)
            .recentPlaceAdded(place.info, isChanged, null)
    }

    private fun currentPlace(): IdeDocumentHistoryImpl.PlaceInfo {
        val virtualFile = myFixture.file.virtualFile
        val editorWithProvider =
            checkNotNull(FileEditorManagerEx.getInstanceEx(myFixture.project).getSelectedEditorWithProvider(virtualFile))
        val offset = myFixture.editor.caretModel.offset
        return IdeDocumentHistoryImpl.PlaceInfo(
            virtualFile,
            editorWithProvider.fileEditor.getState(FileEditorStateLevel.UNDO),
            editorWithProvider.provider.editorTypeId,
            null,
            myFixture.editor.document.createRangeMarker(offset, offset),
        )
    }

    private class Place(val offset: Int, val info: IdeDocumentHistoryImpl.PlaceInfo)
}
