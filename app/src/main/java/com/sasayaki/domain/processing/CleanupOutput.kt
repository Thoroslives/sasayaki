package com.sasayaki.domain.processing

enum class CleanupRejection { TRUNCATED, EMPTY, REPEATED, GREW, SHRANK }

private val reasoningBlock = Regex(
    "<(think|thinking|reasoning)>.*?</\\1>",
    setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
)
private val reasoningClose = Regex("</(think|thinking|reasoning)>", RegexOption.IGNORE_CASE)
private val reasoningOpen = Regex("<(think|thinking|reasoning)>", RegexOption.IGNORE_CASE)
private val codeFence = Regex("^```[\\w-]*\\n(.*)\\n```$", RegexOption.DOT_MATCHES_ALL)
private val wrappingTag = Regex(
    "^<(transcript|text|output|cleaned_text)>\\s*(.*?)\\s*</\\1>$",
    setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
)
private val leadingLabel = Regex(
    "^(?:here(?:'s| is)(?: the| your)? (?:cleaned[ -]up |cleaned |corrected |edited |revised |polished |final )?" +
        "(?:text|transcript|transcription|dictation|version)|" +
        "(?:cleaned[ -]up|cleaned|corrected|edited|revised|polished|final) " +
        "(?:text|transcript|transcription|dictation|version))\\s*:\\s*",
    RegexOption.IGNORE_CASE
)
private val trailingNote = Regex(
    "\\n+\\(?(?:note|notes|changes made|changes)\\s*:.*$",
    setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
)
private val quotePairs = listOf('"' to '"', '“' to '”', '\'' to '\'', '«' to '»', '„' to '“', '「' to '」')

/**
 * stripModelArtifacts removes the reasoning and framing models wrap around an answer.
 * Framing the speaker could plausibly have dictated (a label, quotes, a note) is kept
 * when [input] has it too.
 */
fun stripModelArtifacts(output: String, input: String): String {
    var text = reasoningBlock.replace(output, "")
    // Some chat templates open the reasoning block inside the prompt, so only its end
    // reaches the response.
    reasoningClose.findAll(text).lastOrNull()?.let { text = text.substring(it.range.last + 1) }
    // An opening tag with no end is reasoning cut off by the token limit.
    reasoningOpen.find(text)?.let { text = text.substring(0, it.range.first) }
    text = text.trim()

    codeFence.matchEntire(text)?.let { text = it.groupValues[1].trim() }
    wrappingTag.matchEntire(text)?.let { text = it.groupValues[2] }

    val trimmedInput = input.trim()
    if (!leadingLabel.containsMatchIn(trimmedInput)) text = leadingLabel.replaceFirst(text, "")
    if (!input.contains("note", ignoreCase = true)) text = trailingNote.replace(text, "")

    text = text.trim()
    val wrapped = quotePairs.firstOrNull { (open, close) ->
        text.length >= 2 && text.first() == open && text.last() == close &&
            text.substring(1, text.length - 1).none { it == open || it == close }
    }
    if (wrapped != null && trimmedInput.firstOrNull() != wrapped.first) {
        text = text.substring(1, text.length - 1).trim()
    }
    return text
}

/**
 * rejectCleanup flags outputs that are more likely a model failure than a cleanup: cut
 * off, empty, the transcript echoed twice, or a length change only an answer to the
 * dictation (instead of a cleanup of it) would explain. Lengths are counted in characters
 * so the checks hold for languages written without spaces.
 */
fun rejectCleanup(input: String, output: String, finishReason: String?, condensing: Boolean): CleanupRejection? {
    val inputLength = input.trim().length
    val outputLength = output.trim().length
    return when {
        finishReason == "length" -> CleanupRejection.TRUNCATED
        output.isBlank() -> CleanupRejection.EMPTY
        isRepeated(input, output) -> CleanupRejection.REPEATED
        outputLength > inputLength * MAX_GROWTH + GROWTH_ALLOWANCE -> CleanupRejection.GREW
        !condensing && inputLength >= MIN_LENGTH_FOR_SHRINK_CHECK && outputLength < inputLength * MIN_RETAINED ->
            CleanupRejection.SHRANK
        else -> null
    }
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
