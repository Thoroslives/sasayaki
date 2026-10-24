package com.sasayaki.domain.processing

private const val GLOSSARY_PREFIX = "Glossary: "
const val MAX_VOCABULARY_TERM_LENGTH = 60

// SPEECH_PROMPT_MAX_CHARS keeps the glossary inside the 224 tokens Whisper reads; names
// and jargon split into short tokens, about 2.4 characters each.
private const val SPEECH_PROMPT_MAX_CHARS = 540
private const val CLEANUP_VOCABULARY_MAX_CHARS = 3000

// MIN_RECITED_RUN is how many glossary terms, in glossary order, an unlabelled run at the
// start of a transcript needs before it counts as the prompt echoed back.
private const val MIN_RECITED_RUN = 3

fun parseVocabulary(input: String): List<String> = vocabularyCandidates(input)
    .filter { it.length <= MAX_VOCABULARY_TERM_LENGTH }
    .distinctBy(String::lowercase)

fun hasOverlongTerm(input: String): Boolean = vocabularyCandidates(input).any { it.length > MAX_VOCABULARY_TERM_LENGTH }

private fun vocabularyCandidates(input: String): List<String> =
    input.split(',', ';', '\n').map(String::trim).filter(String::isNotEmpty)

fun sortedVocabulary(terms: Collection<String>): List<String> = terms.sortedBy(String::lowercase)

fun speechPrompt(vocabulary: List<String>): String? {
    val terms = fitting(vocabulary, speechTermsBudget)
    return if (terms.isEmpty()) null else GLOSSARY_PREFIX + terms.joinToString(", ") + "."
}

fun vocabularySection(vocabulary: List<String>): String? {
    val terms = fitting(vocabulary, CLEANUP_VOCABULARY_MAX_CHARS)
    if (terms.isEmpty()) return null
    return "The speaker uses these names and terms. When the transcript has a word that sounds like one of " +
        "them, spell it exactly as listed:\n<vocabulary>\n${terms.joinToString(", ")}\n</vocabulary>"
}

data class VocabularyReach(val speech: Int, val cleanup: Int)

fun vocabularyReach(vocabulary: List<String>): VocabularyReach = VocabularyReach(
    speech = fitting(vocabulary, speechTermsBudget).size,
    cleanup = fitting(vocabulary, CLEANUP_VOCABULARY_MAX_CHARS).size
)

/**
 * withoutRecitedGlossary removes the glossary when the speech model echoes its prompt
 * back, which Whisper-style models do on short or quiet audio. An echo only ever leads
 * the transcript, so a list the speaker dictates later is never touched. [vocabulary]
 * must be in the order [speechPrompt] was given it.
 */
fun withoutRecitedGlossary(transcript: String, vocabulary: List<String>): String {
    if (vocabulary.isEmpty()) return transcript
    val positions = vocabulary.withIndex().associate { (index, term) -> term.lowercase() to index }
    val term = vocabulary.sortedByDescending(String::length).joinToString("|", transform = Regex::escape)
    val leadingRun = Regex("(?iu)^\\s*(glossary:\\s*)?((?:$term)(?:\\s*,\\s*(?:$term))*)(?![\\p{L}\\p{N}])\\.?")
    val match = try {
        leadingRun.find(transcript)
    } catch (_: StackOverflowError) {
        null
    } ?: return transcript

    val labelled = match.groups[1] != null
    val indices = match.groupValues[2].split(',').map { positions[it.trim().lowercase()] ?: -1 }
    val inGlossaryOrder = indices.size >= MIN_RECITED_RUN && indices.zipWithNext().all { (a, b) -> b == a + 1 }
    if (!labelled && !inGlossaryOrder) return transcript
    return transcript.substring(match.range.last + 1).trim()
}

private val speechTermsBudget = SPEECH_PROMPT_MAX_CHARS - GLOSSARY_PREFIX.length - 1

private fun fitting(terms: List<String>, maxChars: Int): List<String> {
    var used = 0
    return terms.takeWhile { term ->
        used += term.length + 2
        used <= maxChars
    }
}
