package com.github.strindberg.emacsj.xref

import com.github.strindberg.emacsj.mark.MarkHandler
import com.github.strindberg.emacsj.mark.PlaceInfo
import com.github.strindberg.emacsj.mark.UndoRedoStack
import com.github.strindberg.emacsj.mark.manager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.CommandEvent
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.intellij.lang.annotations.Language

enum class XRefType { BACK, PUSH, FORWARD }

@Language("devkit-action-id")
internal const val ACTION_XREF_BACK = "com.github.strindberg.emacsj.actions.xref.xrefback"

@Language("devkit-action-id")
internal const val ACTION_XREF_PUSH = "com.github.strindberg.emacsj.actions.xref.xrefpush"

@Language("devkit-action-id")
internal const val ACTION_XREF_FORWARD = "com.github.strindberg.emacsj.actions.xref.xrefforward"

class XRefHandler(private val type: XRefType) : EditorActionHandler() {

    companion object {
        internal val xRefCommandNames = setOf(
            "Go to Declaration or Usages",
            "Go to Declaration",
            "Go to Type Declaration",
        )

        private val places = mutableMapOf<String, UndoRedoStack<PlaceInfo>>()

        internal fun pushPlace(event: CommandEvent) {
            event.project?.let { project ->
                project.manager?.let { manager ->
                    manager.selectedFiles.getOrNull(0)?.let { virtualFile ->
                        (manager.getSelectedEditor(virtualFile) as? TextEditor)?.let { fileEditor ->
                            pushPlaceInfo(fileEditor.editor, project, virtualFile)
                        }
                    }
                }
            }
        }

        @Volatile
        private var isNavigationExpected = false

        /**
         * Announces navigation to a definition, an implementation or a usage that is about to be made from somewhere
         * other than a command, such as a click on a usage in the *Find Usages* tool window. The place left behind is
         * not known until the platform has recorded it, in [pushNavigationPlace].
         */
        internal fun expectNavigation() {
            isNavigationExpected = true
        }

        /**
         * Called before a command that navigates to a definition, an implementation or a usage. The caret has not
         * moved yet, so the place to remember is the current one. A command that opens a popup of candidates counts as
         * well: whichever entry is picked, the navigation starts out from here.
         */
        internal fun pushNavigationPlace(editor: Editor, project: Project) {
            (editor as? EditorEx)?.virtualFile?.let { virtualFile ->
                MarkHandler.placeInfo(editor, virtualFile)?.let { place ->
                    pushOnce(place, project)
                }
            }
        }

        /**
         * Called for places that the platform records as navigated away from. Announced navigation is awaited, since
         * the platform records these places for every kind of navigation, ordinary caret movement included.
         */
        internal fun pushAnnouncedPlace(place: PlaceInfo, project: Project) {
            if (MarkHandler.isNavigatingToPlace || !isNavigationExpected) return

            isNavigationExpected = false

            pushOnce(place, project)
        }

        /**
         * A single navigation can be seen both as a command and as a place recorded by the platform, and is pushed
         * only once.
         */
        private fun pushOnce(place: PlaceInfo, project: Project) {
            places.getOrPut(project.name) { UndoRedoStack() }.let { stack ->
                if (stack.peek() != place) {
                    stack.push(place)
                }
            }
        }

        private fun getPlaceForBackAction(editor: Editor): PlaceInfo? =
            getPlaceUsingHistory(editor) { stack, current -> stack.undo(current) }

        private fun getPlaceForForwardAction(editor: Editor): PlaceInfo? =
            getPlaceUsingHistory(editor) { stack, current -> stack.redo(current) }

        private fun pushPlace(editor: EditorEx) {
            editor.project?.let { project ->
                pushPlaceInfo(editor, project, editor.virtualFile)
            }
        }

        private fun pushPlaceInfo(editor: Editor, project: Project, virtualFile: VirtualFile) {
            MarkHandler.placeInfo(editor, virtualFile)?.let {
                places.getOrPut(project.name) { UndoRedoStack() }.push(it)
            }
        }

        private fun getPlaceUsingHistory(editor: Editor, operation: (UndoRedoStack<PlaceInfo>, PlaceInfo) -> PlaceInfo?): PlaceInfo? =
            editor.project?.let { project ->
                places[project.name]?.let { stack ->
                    editor.virtualFile?.let { currentFile ->
                        MarkHandler.placeInfo(editor, currentFile)?.let { currentPlace ->
                            operation(stack, currentPlace)
                        }
                    }
                }
            }
    }

    override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
        if (editor is EditorEx) {
            when (type) {
                XRefType.BACK -> getPlaceForBackAction(editor)?.let { place ->
                    MarkHandler.gotoPlaceInfo(editor, place)
                }
                XRefType.FORWARD -> getPlaceForForwardAction(editor)?.let { place ->
                    MarkHandler.gotoPlaceInfo(editor, place)
                }
                XRefType.PUSH -> pushPlace(editor)
            }
        }
    }
}
