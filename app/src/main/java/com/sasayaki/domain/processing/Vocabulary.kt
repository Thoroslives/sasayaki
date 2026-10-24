package com.sasayaki.domain.processing

private const val GLOSSARY_PREFIX = "Glossary: "
const val MAX_VOCABULARY_TERM_LENGTH = 60

// Whisper keeps only the last 224 tokens of its prompt, roughly four characters each.
private const val SPEECH_PROMPT_MAX_CHARS = 900
private const val CLEANUP_VOCABULARY_MAX_CHARS = 3000

// A comma-separated run of this many glossary terms, in glossary order, is the speech
// model reciting its prompt rather than the speaker listing them.
private const val MIN_RECITED_RUN = 3

private val recitedGlossaryLabel = Regex("(?i)glossary:[^.\\n]*\\.?")

fun parseVocabulary(input: String): List<String> = input
    .split(',', ';', '\n')
    .map(String::trim)
    .filter { it.isNotEmpty() && it.length <= MAX_VOCABULARY_TERM_LENGTH }
    .distinctBy(String::lowercase)

fun sortedVocabulary(terms: Collection<String>): List<String> = terms.sortedBy(String::lowercase)

fun speechPrompt(vocabulary: List<String>): String? {
    val terms = fitting(vocabulary, SPEECH_PROMPT_MAX_CHARS - GLOSSARY_PREFIX.length - 1)
    return if (terms.isEmpty()) null else GLOSSARY_PREFIX + terms.joinToString(", ") + "."
}

fun vocabularySection(vocabulary: List<String>): String? {
    val terms = fitting(vocabulary, CLEANUP_VOCABULARY_MAX_CHARS)
    if (terms.isEmpty()) return null
    return "The speaker uses these names and terms. When the transcript has a word that sounds like one of " +
        "them, spell it exactly as listed:\n<vocabulary>\n${terms.joinToString(", ")}\n</vocabulary>"
}

/**
 * withoutRecitedGlossary removes the glossary when the speech model echoes its prompt
 * back, which Whisper-style models do on short or quiet audio. [vocabulary] must be in
 * the order [speechPrompt] was given it.
 */
fun withoutRecitedGlossary(transcript: String, vocabulary: List<String>): String {
    var text = recitedGlossaryLabel.replace(transcript, "")
    if (vocabulary.size >= MIN_RECITED_RUN) {
        val positions = vocabulary.withIndex().associate { (index, term) -> term.lowercase() to index }
        val term = vocabulary.sortedByDescending(String::length).joinToString("|", transform = Regex::escape)
        val run = Regex("(?i)(?<![\\p{L}\\p{N}])(?:$term)(?:\\s*,\\s*(?:$term))+(?![\\p{L}\\p{N}])\\.?")
        text = run.replace(text) { match ->
            val indices = match.value.trimEnd('.').split(',').map { positions[it.trim().lowercase()] ?: -1 }
            val recited = indices.size >= MIN_RECITED_RUN && indices.zipWithNext().all { (a, b) -> b == a + 1 }
            if (recited) "" else match.value
        }
    }
    return if (text == transcript) transcript else text.replace(Regex(" {2,}"), " ").trim()
}

private fun fitting(terms: List<String>, maxChars: Int): List<String> {
    var used = 0
    return terms.takeWhile { term ->
        used += term.length + 2
        used <= maxChars
    }
}
