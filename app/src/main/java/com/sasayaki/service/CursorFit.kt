package com.sasayaki.service

import java.lang.Character.UnicodeScript

private const val CONTINUING_MARKS = ",;"
private const val OPENING_MARKS = "([{“‘«¿¡@#/"
private const val SYMMETRIC_QUOTES = "\"'"
private const val TIGHT_LEADING_MARKS = ".,;:!?)]}…%»”’"
private const val FULLWIDTH_MARKS = "。、，．！？：；）」』】〉》"
private val unspacedScripts = setOf(UnicodeScript.HAN, UnicodeScript.HIRAGANA, UnicodeScript.KATAKANA, UnicodeScript.THAI)

// lowercaseStarts are words that stay lowercase mid-sentence in the languages Sasayaki is
// tuned for. Lowering only these leaves names, German nouns and "Sie" alone, none of
// which can be told apart from a sentence-initial capital otherwise.
private val lowercaseStarts = setOf(
    "the", "a", "an", "and", "but", "or", "so", "then", "because", "if", "when", "while", "which", "that",
    "this", "these", "those", "it", "its", "we", "you", "he", "she", "they", "my", "your", "our", "their",
    "his", "her", "there", "here", "to", "in", "on", "at", "for", "with", "of", "as", "just", "also",
    "maybe", "please", "not", "no", "yes", "what", "how", "why", "where", "who",
    "el", "la", "los", "las", "un", "una", "y", "pero", "o", "que", "porque", "si", "cuando", "es", "yo",
    "en", "con", "de", "por", "para", "también", "pues",
    "le", "les", "une", "et", "mais", "ou", "donc", "parce", "quand", "je", "tu", "il", "elle", "nous",
    "vous", "ils", "elles", "avec", "pour", "pas", "oui", "non",
    "lo", "gli", "e", "ma", "che", "perché", "se", "io", "noi", "voi", "di", "per",
    "os", "as", "um", "uma", "mas", "eu", "você", "não",
    "els", "però", "perquè", "quan", "jo", "amb",
    "het", "een", "en", "maar", "of", "dat", "als", "ik", "wij", "zij", "met", "van", "voor", "niet",
    "und", "aber", "oder", "dass", "wenn", "weil", "ich", "wir", "der", "die", "das", "ein", "eine", "mit",
    "von", "für", "nicht"
)

/**
 * fitToCursor adapts a dictation, which is cleaned as a standalone sentence, to the text
 * it lands after: a space where it would otherwise run into the previous word, and a
 * lowered first word when it continues a sentence after a comma. A null
 * [textBeforeCursor] means the editor would not say, so the dictation is left as it came.
 */
fun fitToCursor(textBeforeCursor: String?, dictated: String): String {
    textBeforeCursor ?: return dictated
    if (textBeforeCursor.isEmpty()) return dictated.trimStart()
    val text = dictated.trimStart(' ', '\t')
    // A dictated new line already separates the text and starts afresh.
    if (text.isEmpty() || text.first() == '\n') return text

    val spaced = if (needsSpace(textBeforeCursor, text.first())) " $text" else text
    return if (continuesSentence(textBeforeCursor)) lowerFirstWord(spaced) else spaced
}

/**
 * textBeforeCursor reads an editor's surrounding text, returning null when the offsets do
 * not describe a position in it.
 */
fun textBeforeCursor(text: CharSequence?, selectionStart: Int, selectionEnd: Int): String? {
    text ?: return null
    val cursor = minOf(selectionStart, selectionEnd)
    if (cursor < 0 || cursor > text.length) return null
    return text.subSequence(0, cursor).toString()
}

private fun needsSpace(before: String, first: Char): Boolean {
    val last = before.last()
    if (last.isWhitespace() || last in OPENING_MARKS || last in FULLWIDTH_MARKS || first in TIGHT_LEADING_MARKS) {
        return false
    }
    if (isUnspaced(last) || isUnspaced(first)) return false
    // A straight quote after a space opens a quotation; after a word it closes one.
    if (last in SYMMETRIC_QUOTES) return before.length > 1 && !before[before.length - 2].isWhitespace()
    return true
}

private fun isUnspaced(char: Char): Boolean = UnicodeScript.of(char.code) in unspacedScripts

private fun continuesSentence(before: String): Boolean {
    val trailingSpace = before.takeLastWhile(Char::isWhitespace)
    if ('\n' in trailingSpace) return false
    return before.lastOrNull { !it.isWhitespace() }?.let { it in CONTINUING_MARKS } == true
}

private fun lowerFirstWord(text: String): String {
    val start = text.indexOfFirst { !it.isWhitespace() }
    if (start < 0 || !text[start].isUpperCase()) return text
    val word = text.substring(start).takeWhile(Char::isLetter)
    if (word.lowercase() !in lowercaseStarts || word.drop(1).any(Char::isUpperCase)) return text
    return text.substring(0, start) + text[start].lowercaseChar() + text.substring(start + 1)
}
