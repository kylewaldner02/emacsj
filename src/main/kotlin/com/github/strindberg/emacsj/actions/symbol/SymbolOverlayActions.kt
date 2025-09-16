package com.github.strindberg.emacsj.actions.symbol

import com.github.strindberg.emacsj.symbol.SymbolOverlayHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorAction
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import org.intellij.lang.annotations.Language

@Language("devkit-action-id")
internal const val ACTION_SYMBOL_OVERLAY_PUT = "com.github.strindberg.emacsj.actions.symbol.symboloverlayput"

@Language("devkit-action-id")
internal const val ACTION_SYMBOL_OVERLAY_JUMP_NEXT = "com.github.strindberg.emacsj.actions.symbol.symboloverlayjumpnext"

@Language("devkit-action-id")
internal const val ACTION_SYMBOL_OVERLAY_JUMP_PREV = "com.github.strindberg.emacsj.actions.symbol.symboloverlayjumpprev"

@Language("devkit-action-id")
internal const val ACTION_SYMBOL_OVERLAY_REMOVE_ALL = "com.github.strindberg.emacsj.actions.symbol.symboloverlayremoveall"

/**
 * Action to highlight symbol at point and enable symbol overlay navigation
 */
class SymbolOverlayPutAction :
    EditorAction(SymbolOverlayPutHandler()),
    SymbolOverlayAction

/**
 * Action to jump to next occurrence of highlighted symbol
 */
class SymbolOverlayJumpNextAction :
    EditorAction(SymbolOverlayJumpNextHandler()),
    SymbolOverlayAction

/**
 * Action to jump to previous occurrence of highlighted symbol
 */
class SymbolOverlayJumpPrevAction :
    EditorAction(SymbolOverlayJumpPrevHandler()),
    SymbolOverlayAction

/**
 * Action to remove all symbol overlays
 */
class SymbolOverlayRemoveAllAction :
    EditorAction(SymbolOverlayRemoveAllHandler()),
    SymbolOverlayAction

/**
 * Handler for highlighting symbol at point
 */
class SymbolOverlayPutHandler : EditorActionHandler() {
    override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
        val handler = SymbolOverlayHandler(editor)
        val success = handler.highlightSymbolAtPoint()

        if (!success) {
            // Could show a message or beep to indicate no symbol found
            return
        }
    }
}

/**
 * Handler for jumping to next symbol occurrence
 */
class SymbolOverlayJumpNextHandler : EditorActionHandler() {
    override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
        val handler = SymbolOverlayHandler(editor)
        handler.jumpNext()
    }
}

/**
 * Handler for jumping to previous symbol occurrence
 */
class SymbolOverlayJumpPrevHandler : EditorActionHandler() {
    override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
        val handler = SymbolOverlayHandler(editor)
        handler.jumpPrev()
    }
}

/**
 * Handler for removing all symbol overlays
 */
class SymbolOverlayRemoveAllHandler : EditorActionHandler() {
    override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
        val handler = SymbolOverlayHandler(editor)
        handler.clearHighlights()
    }
}

/**
 * Marker interface for symbol overlay actions
 */
interface SymbolOverlayAction
