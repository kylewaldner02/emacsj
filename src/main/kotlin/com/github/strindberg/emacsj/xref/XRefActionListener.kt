package com.github.strindberg.emacsj.xref

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.IdeActions.ACTION_FIND_USAGES
import com.intellij.openapi.actionSystem.IdeActions.ACTION_GOTO_DECLARATION
import com.intellij.openapi.actionSystem.IdeActions.ACTION_GOTO_IMPLEMENTATION
import com.intellij.openapi.actionSystem.IdeActions.ACTION_GOTO_SUPER
import com.intellij.openapi.actionSystem.IdeActions.ACTION_GOTO_TYPE_DECLARATION
import com.intellij.openapi.actionSystem.ex.AnActionListener

/**
 * Commands that navigate to a definition, an implementation or a usage. Navigation to an implementation or to a usage
 * shown in a popup does not go through the navigation requests that [XRefNavigationHandler] sees, so the command
 * itself is what announces it. The announcement holds until the navigation happens, which for a popup is when the user
 * picks one of the entries in it.
 */
private val NAVIGATION_ACTIONS = setOf(
    ACTION_GOTO_DECLARATION,
    "GotoDeclarationOnly",
    ACTION_GOTO_TYPE_DECLARATION,
    ACTION_GOTO_IMPLEMENTATION,
    ACTION_GOTO_SUPER,
    ACTION_FIND_USAGES,
    "ShowUsages",
)

class XRefActionListener : AnActionListener {

    override fun beforeActionPerformed(action: AnAction, event: AnActionEvent) {
        if (ActionManager.getInstance().getId(action) in NAVIGATION_ACTIONS) {
            event.getData(CommonDataKeys.EDITOR)?.let { editor ->
                event.project?.let { project ->
                    XRefHandler.pushNavigationPlace(editor, project)
                }
            }
        }
    }
}
