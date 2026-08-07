package com.github.strindberg.emacsj.xref

import com.github.strindberg.emacsj.mark.PlaceInfo
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl.RecentPlacesListener
import com.intellij.openapi.project.Project

/**
 * Pushes places onto the XRef stack for navigation that doesn't go through a command of its own, such as clicking a
 * usage or an implementation with the mouse. Those places are the ones the platform records for its own Go Back
 * action.
 */
class XRefPlacesListener(private val project: Project) : RecentPlacesListener {

    override fun recentPlaceAdded(changePlace: IdeDocumentHistoryImpl.PlaceInfo, isChanged: Boolean, groupId: Any?) {
        if (!isChanged) {
            placeInfo(changePlace)?.let { place ->
                XRefHandler.pushAnnouncedPlace(place, project)
            }
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun recentPlaceAdded(changePlace: IdeDocumentHistoryImpl.PlaceInfo, isChanged: Boolean) =
        recentPlaceAdded(changePlace, isChanged, null)

    override fun recentPlaceRemoved(changePlace: IdeDocumentHistoryImpl.PlaceInfo, isChanged: Boolean) = Unit

    private fun placeInfo(place: IdeDocumentHistoryImpl.PlaceInfo): PlaceInfo? =
        place.caretPosition?.takeIf { it.isValid }?.let { caretPosition ->
            PlaceInfo(
                file = place.file,
                state = place.navigationState,
                editorTypeId = place.editorTypeId,
                caretPosition = caretPosition.startOffset,
                scrollOffset = null,
            )
        }
}
