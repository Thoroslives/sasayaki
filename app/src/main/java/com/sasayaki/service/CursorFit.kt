package com.sasayaki.service

private const val SENTENCE_ENDS = ".!?…。！？"
private const val MID_SENTENCE_MARKS = ",;"
private const val OPENING_MARKS = "([{“‘«¿¡"
private const val SYMMETRIC_QUOTES = "\"'"
private const val TIGHT_LEADING_MARKS = ".,;:!?)]}…%»”’"

/**
 * fitToCursor adapts a dictation, which is cleaned as a standalone sentence, to the text
 * it lands after: a space where it would otherwise run into the previous word, and no
 * capital when it continues a sentence. A null [textBeforeCursor] means the editor would
 * not say, so the dictation is left as it came.
 *
 * A capital that starts a proper noun is lowered too when continuing a sentence; nothing
 * in the text tells it apart from a sentence-initial capital.
 */
fun fitToCursor(textBeforeCursor: String?, dictated: String): String {
    textBeforeCursor ?: return dictated
    val text = dictated.trimStart()
    if (text.isEmpty() || textBeforeCursor.isEmpty()) return text

    val spaced = if (needsSpace(textBeforeCursor, text.first())) " $text" else text
    return if (continuesSentence(textBeforeCursor)) lowerFirstWord(spaced) else spaced
}

private fun needsSpace(before: String, first: Char): Boolean {
    val last = before.last()
    if (last.isWhitespace() || last in OPENING_MARKS || first in TIGHT_LEADING_MARKS) return false
    // A straight quote after a space opens a quotation; after a word it closes one.
    if (last in SYMMETRIC_QUOTES) return before.length > 1 && !before[before.length - 2].isWhitespace()
    return true
}

private fun continuesSentence(before: String): Boolean {
    val trailingSpace = before.takeLastWhile(Char::isWhitespace)
    if ('\n' in trailingSpace) return false
    val last = before.lastOrNull { !it.isWhitespace() } ?: return false
    return last.isLetterOrDigit() || last in MID_SENTENCE_MARKS
}

private fun lowerFirstWord(text: String): String {
    val start = text.indexOfFirst(Char::isLetter)
    if (start < 0 || !text[start].isUpperCase()) return text
    val word = text.substring(start).takeWhile { it.isLetter() || it == '\'' || it == '’' }
    val isPronounI = word == "I" || word.startsWith("I'") || word.startsWith("I’")
    val isAcronym = word.length > 1 && word[1].isUpperCase()
    if (isPronounI || isAcronym) return text
    return text.substring(0, start) + text[start].lowercaseChar() + text.substring(start + 1)
}
