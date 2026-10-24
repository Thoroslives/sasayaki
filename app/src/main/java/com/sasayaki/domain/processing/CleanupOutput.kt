package com.sasayaki.domain.processing

import com.sasayaki.domain.model.Profile
import com.sasayaki.domain.model.SummarizeMode

enum class CleanupRejection { TRUNCATED, EMPTY, REPEATED, GREW, SHRANK }

private val reasoningBlock = Regex(
    "<(think|thinking|reasoning)>.*?</\\1>",
    setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
)
private val reasoningClose = Regex("</(think|thinking|reasoning)>", RegexOption.IGNORE_CASE)
private val reasoningOpen = Regex("<(think|thinking|reasoning)>", RegexOption.IGNORE_CASE)
private const val HARMONY_FINAL = "<|channel|>final<|message|>"
private const val HARMONY_CHANNEL = "<|channel|>"
private val codeFence = Regex("^```[\\w-]*\\n(.*)\\n```$", RegexOption.DOT_MATCHES_ALL)
private val wrappingTag = Regex(
    "^<(transcript|text|output|cleaned_text)>\\s*(.*?)\\s*</\\1>$",
    setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
)
private const val LABEL_ADJECTIVES = "cleaned[ -]up|cleaned|corrected|edited|revised|polished|final"
private const val LABEL_NOUNS = "text|transcript|transcription|dictation|version"
private const val LABEL =
    "(?:here(?:'s| is)(?: the| your)? (?:(?:$LABEL_ADJECTIVES) )?(?:$LABEL_NOUNS)|(?:$LABEL_ADJECTIVES) (?:$LABEL_NOUNS))"
private val leadingLabel = Regex("^$LABEL\\s*:\\s*", RegexOption.IGNORE_CASE)
// spokenLabel ignores the colon, which a transcript holds as a word or not at all.
private val spokenLabel = Regex("^$LABEL\\b", RegexOption.IGNORE_CASE)
private val trailingNote = Regex("\\n+\\(?notes?\\s*:(.*)$", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
private val quotePairs = listOf('"' to '"', '“' to '”', '\'' to '\'', '«' to '»', '„' to '“', '「' to '」')
private val dictatedQuote =
    Regex("[\"“”«»„「」]|quot|comillas|guillemets|virgolett|aspas|cometes|anführungszeichen", RegexOption.IGNORE_CASE)

/**
 * stripModelArtifacts removes the reasoning and framing models wrap around an answer.
 * Framing the speaker could have dictated (a label, quotes, a note) is kept when [input]
 * has it too, in written or spoken form. Untouched output keeps its line breaks, which
 * may be a dictated new line.
 */
fun stripModelArtifacts(output: String, input: String): String {
    var text = reasoningBlock.replace(output, "")
    // Some chat templates open the reasoning block inside the prompt, so only its end
    // reaches the response.
    reasoningClose.findAll(text).lastOrNull()?.let { text = text.substring(it.range.last + 1) }
    // An opening tag with no end is reasoning cut off by the token limit.
    reasoningOpen.find(text)?.let { text = text.substring(0, it.range.first) }
    // Servers that pass gpt-oss output through unparsed send its reasoning channel too.
    if (HARMONY_FINAL in text) {
        text = text.substringAfterLast(HARMONY_FINAL).substringBefore("<|")
    } else if (HARMONY_CHANNEL in text) {
        text = ""
    }
    text = text.trim()

    codeFence.matchEntire(text)?.takeUnless { "```" in it.groupValues[1] }?.let { text = it.groupValues[1].trim() }
    wrappingTag.matchEntire(text)?.let { text = it.groupValues[2] }

    val trimmedInput = input.trim()
    if (!spokenLabel.containsMatchIn(trimmedInput)) text = leadingLabel.replaceFirst(text, "")
    trailingNote.find(text)?.let { note ->
        val noteWords = normalizedWords(note.groupValues[1])
        val inputWords = normalizedWords(input).toSet()
        val dictated = noteWords.count { it in inputWords }
        if (dictated * 2 < noteWords.size) text = text.substring(0, note.range.first)
    }

    text = text.trim()
    val wrapped = quotePairs.firstOrNull { (open, close) ->
        text.length >= 2 && text.first() == open && text.last() == close &&
            text.substring(1, text.length - 1).none { it == open || it == close }
    }
    val quoteDictated = dictatedQuote.containsMatchIn(trimmedInput) || trimmedInput.startsWith('\'')
    if (wrapped != null && !quoteDictated) {
        text = text.substring(1, text.length - 1).trim()
    }
    return if (text == output.trim()) output.trim(' ', '\t') else text
}

/**
 * LengthCheck says how far a cleanup may move away from the transcript's length. Custom
 * prompts may legitimately translate or expand, condensing legitimately shrinks.
 */
enum class LengthCheck { FULL, GROWTH_ONLY, NONE }

/**
 * rejectCleanup flags outputs that are more likely a model failure than a cleanup: cut
 * off, empty, the transcript echoed twice, or a length change only an answer to the
 * dictation (instead of a cleanup of it) would explain. Lengths are counted in characters
 * so the checks hold for languages written without spaces.
 */
fun rejectCleanup(input: String, output: String, finishReason: String?, lengthCheck: LengthCheck): CleanupRejection? {
    val inputLength = input.trim().length
    val outputLength = output.trim().length
    return when {
        finishReason == "length" -> CleanupRejection.TRUNCATED
        output.isBlank() -> CleanupRejection.EMPTY
        isRepeated(input, output) -> CleanupRejection.REPEATED
        lengthCheck != LengthCheck.NONE && outputLength > inputLength * MAX_GROWTH + GROWTH_ALLOWANCE ->
            CleanupRejection.GREW
        lengthCheck == LengthCheck.FULL && inputLength >= MIN_LENGTH_FOR_SHRINK_CHECK &&
            outputLength < inputLength * MIN_RETAINED -> CleanupRejection.SHRANK
        else -> null
    }
}

fun lengthCheckFor(profile: Profile): LengthCheck = when {
    profile.profilePrompt.isNotBlank() || profile.selectedPromptIds.isNotEmpty() -> LengthCheck.NONE
    profile.summarizeMode != SummarizeMode.NONE -> LengthCheck.GROWTH_ONLY
    else -> LengthCheck.FULL
}

private fun isRepeated(input: String, output: String): Boolean {
    val words = normalizedWords(output)
    if (words.size >= MIN_WORDS_FOR_REPEAT_CHECK && words.size % 2 == 0) {
        val half = words.size / 2
        if (words.subList(0, half) == words.subList(half, words.size)) return true
    }
    val inputWords = normalizedWords(input)
    if (inputWords.size < MIN_WORDS_FOR_REPEAT_CHECK / 2) return false
    val needle = inputWords.joinToString(" ")
    val haystack = words.joinToString(" ")
    val first = haystack.indexOf(needle)
    return first >= 0 && haystack.indexOf(needle, first + needle.length) >= 0
}

private fun normalizedWords(text: String): List<String> =
    text.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter(String::isNotEmpty)

private const val MAX_GROWTH = 2
private const val GROWTH_ALLOWANCE = 60
private const val MIN_LENGTH_FOR_SHRINK_CHECK = 120
private const val MIN_RETAINED = 0.35
private const val MIN_WORDS_FOR_REPEAT_CHECK = 8
