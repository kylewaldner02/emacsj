package com.github.strindberg.emacsj.xref

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.platform.backend.navigation.NavigationRequest
import com.intellij.platform.backend.navigation.impl.RawNavigationRequest
import com.intellij.platform.backend.navigation.impl.SourceNavigationRequest
import com.intellij.platform.ide.navigation.NavigationHandler
import com.intellij.platform.ide.navigation.NavigationOptions
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiFileSystemItem

/**
 * Notices navigation to a specific position in a file, such as a declaration, an implementation or a usage, however it
 * is invoked: from a keyboard command, from a click in the *Find Usages* tool window, or from a click in one of the
 * popups showing usages or implementations.
 *
 * Navigation that merely opens a file, such as selecting a file in the project view or among recent files, carries no
 * position and is ignored, and so never reaches the XRef stack.
 *
 * The place navigated away from is not read here, since the request is computed on a background thread. It is instead
 * picked up by [XRefPlacesListener] once the platform has recorded it.
 */
class XRefNavigationHandler : NavigationHandler {

    override fun navigate(request: NavigationRequest, options: NavigationOptions, dataContext: DataContext): Boolean {
        if (request.isNavigationToPosition) {
            XRefHandler.expectNavigation()
        }
        return false
    }
}

/**
 * A request carries a position when it holds an offset within a file, or when it points out an element rather than a
 * file. Requests for a file as a whole, or for a directory, carry no position.
 */
private val NavigationRequest.isNavigationToPosition: Boolean
    get() = when (this) {
        is SourceNavigationRequest -> offsetMarker != null
        is RawNavigationRequest -> navigatable.isPosition
        else -> false
    }

private val Navigatable.isPosition: Boolean
    get() = when (this) {
        is PsiFileSystemItem -> false
        is OpenFileDescriptor -> offset >= 0
        else -> true
    }
