package com.github.strindberg.emacsj.symbol

import com.intellij.find.FindManager
import com.intellij.find.FindModel
import com.intellij.find.FindResult
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType.MAKE_VISIBLE
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory

/**
 * Handler for symbol overlay functionality similar to Emacs symbol-overlay package.
 * Provides navigation between symbol occurrences without highlighting.
 */
class SymbolOverlayHandler(private val editor: Editor) {

    /**
     * Jumps to the next occurrence of the symbol at the current caret position
     */
    fun jumpNext(): Boolean {
        val caret = editor.caretModel.primaryCaret
        val symbol = getSymbolAtPoint(caret.offset) ?: return false

        val occurrences = findSymbolOccurrences(symbol)
        if (occurrences.isEmpty()) return false

        val currentIndex = findCurrentOccurrenceIndex(caret.offset, occurrences)
        val nextIndex = (currentIndex + 1) % occurrences.size

        // Calculate relative position within current symbol
        val currentOccurrence = occurrences[currentIndex]
        val relativePosition = caret.offset - currentOccurrence.startOffset

        jumpToOccurrence(occurrences[nextIndex], relativePosition)
        return true
    }

    /**
     * Jumps to the previous occurrence of the symbol at the current caret position
     */
    fun jumpPrev(): Boolean {
        val caret = editor.caretModel.primaryCaret
        val symbol = getSymbolAtPoint(caret.offset) ?: return false

        val occurrences = findSymbolOccurrences(symbol)
        if (occurrences.isEmpty()) return false

        val currentIndex = findCurrentOccurrenceIndex(caret.offset, occurrences)
        val prevIndex = if (currentIndex == 0) occurrences.size - 1 else currentIndex - 1

        // Calculate relative position within current symbol
        val currentOccurrence = occurrences[currentIndex]
        val relativePosition = caret.offset - currentOccurrence.startOffset

        jumpToOccurrence(occurrences[prevIndex], relativePosition)
        return true
    }

    /**
     * Highlights all occurrences of the symbol at the current caret position
     */
    fun highlightSymbolAtPoint(): Boolean {
        val caret = editor.caretModel.primaryCaret
        val symbol = getSymbolAtPoint(caret.offset) ?: return false

        clearHighlights()
        val occurrences = findSymbolOccurrences(symbol)
        if (occurrences.isEmpty()) return false

        highlightAllOccurrences(occurrences)
        return true
    }

    /**
     * Clears all symbol highlights
     */
    fun clearHighlights() {
        editor.markupModel.removeAllHighlighters()
    }

    /**
     * Gets the symbol at the specified offset
     */
    private fun getSymbolAtPoint(offset: Int): String? {
        val document = editor.document
        val text = document.text

        if (offset >= text.length) return null

        // Find word boundaries
        var start = offset
        var end = offset

        // Move start backward to beginning of word
        while (start > 0 && isWordCharacter(text[start - 1])) {
            start--
        }

        // Move end forward to end of word
        while (end < text.length && isWordCharacter(text[end])) {
            end++
        }

        return if (start < end) text.substring(start, end) else null
    }

    /**
     * Checks if a character is part of a word (identifier)
     */
    private fun isWordCharacter(char: Char): Boolean = char.isLetterOrDigit() || char == '_'

    /**
     * Finds all occurrences of the given symbol in the document
     */
    private fun findSymbolOccurrences(symbol: String): List<FindResult> {
        val occurrences = mutableListOf<FindResult>()

        val findManager = FindManager.getInstance(editor.project)
        val findModel = FindModel().apply {
            stringToFind = symbol
            isCaseSensitive = true
            isRegularExpressions = false
        }

        val text = editor.document.text
        var offset = 0

        while (offset < text.length) {
            val result = findManager.findString(text, offset, findModel)
            if (!result.isStringFound) break

            occurrences.add(result)
            offset = maxOf(result.endOffset, offset + 1)
        }

        return occurrences
    }

    /**
     * Finds the index of the occurrence that contains the given offset
     */
    private fun findCurrentOccurrenceIndex(caretOffset: Int, occurrences: List<FindResult>): Int {
        return occurrences.indexOfFirst { occurrence ->
            caretOffset >= occurrence.startOffset && caretOffset <= occurrence.endOffset
        }.takeIf { it >= 0 } ?: 0
    }

    /**
     * Highlights all symbol occurrences
     */
    private fun highlightAllOccurrences(occurrences: List<FindResult>) {
        occurrences.forEach { occurrence ->
            editor.markupModel.addRangeHighlighter(
                com.github.strindberg.emacsj.search.EMACSJ_SECONDARY,
                occurrence.startOffset,
                occurrence.endOffset,
                com.intellij.openapi.editor.markup.HighlighterLayer.LAST + 1,
                com.intellij.openapi.editor.markup.HighlighterTargetArea.EXACT_RANGE
            )
        }
    }

    /**
     * Jumps to the specified occurrence with preserved relative position
     */
    private fun jumpToOccurrence(occurrence: FindResult, relativePosition: Int = 0) {
        // Calculate target position, ensuring it doesn't go beyond the symbol bounds
        val symbolLength = occurrence.endOffset - occurrence.startOffset
        val targetPosition = occurrence.startOffset + minOf(relativePosition, symbolLength)

        // Move caret to the calculated position
        editor.caretModel.moveToOffset(targetPosition)
        editor.scrollingModel.scrollToCaret(MAKE_VISIBLE)

        // Add to navigation history
        IdeDocumentHistory.getInstance(editor.project).includeCurrentCommandAsNavigation()
    }
}
