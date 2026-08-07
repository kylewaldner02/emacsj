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
    fun jumpNext(): Boolean = jump { currentIndex, size -> (currentIndex + 1) % size }

    /**
     * Jumps to the previous occurrence of the symbol at the current caret position
     */
    fun jumpPrev(): Boolean = jump { currentIndex, size -> if (currentIndex == 0) size - 1 else currentIndex - 1 }

    private fun jump(indexToJumpTo: (currentIndex: Int, size: Int) -> Int): Boolean {
        val caret = editor.caretModel.primaryCaret
        val occurrences = getSymbolAtPoint(caret.offset)?.let { findSymbolOccurrences(it) }.orEmpty()

        if (occurrences.isEmpty()) return false

        val currentIndex = findCurrentOccurrenceIndex(caret.offset, occurrences)

        // Calculate relative position within current symbol
        val currentOccurrence = occurrences[currentIndex]
        val relativePosition = caret.offset - currentOccurrence.startOffset

        jumpToOccurrence(occurrences[indexToJumpTo(currentIndex, occurrences.size)], relativePosition)
        return true
    }

    /**
     * Highlights all occurrences of the symbol at the current caret position
     */
    fun highlightSymbolAtPoint(): Boolean {
        val occurrences = getSymbolAtPoint(editor.caretModel.primaryCaret.offset)?.let { symbol ->
            clearHighlights()
            findSymbolOccurrences(symbol)
        }.orEmpty()

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

            if (text.isWholeSymbolAt(result.startOffset, result.endOffset)) {
                occurrences.add(result)
            }
            offset = maxOf(result.endOffset, offset + 1)
        }

        return occurrences
    }

    /**
     * A match counts as an occurrence of the symbol only when it isn't part of a longer word. Jumping between the
     * occurrences of `downloadExportUseCase` must not stop at `downloadExportUseCaseTest`.
     */
    private fun CharSequence.isWholeSymbolAt(startOffset: Int, endOffset: Int): Boolean =
        (startOffset == 0 || !isWordCharacter(this[startOffset - 1])) &&
            (endOffset >= length || !isWordCharacter(this[endOffset]))

    /**
     * Finds the index of the occurrence that contains the given offset
     */
    private fun findCurrentOccurrenceIndex(caretOffset: Int, occurrences: List<FindResult>): Int =
        occurrences.indexOfFirst { occurrence ->
            caretOffset >= occurrence.startOffset && caretOffset <= occurrence.endOffset
        }.takeIf { it >= 0 } ?: 0

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
