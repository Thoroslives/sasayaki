package com.sasayaki.domain.processing

import com.sasayaki.domain.model.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyTest {

    private val vocabulary = sortedVocabulary(listOf("Kubernetes", "Grafana", "Tailscale", "vLLM"))

    @Test
    fun `input is split, trimmed and deduplicated`() {
        assertEquals(listOf("Grafana", "vLLM", "Mika"), parseVocabulary(" Grafana, vLLM;\nMika, grafana ,, mika"))
        assertEquals(emptyList<String>(), parseVocabulary("x".repeat(MAX_VOCABULARY_TERM_LENGTH + 1)))
    }

    @Test
    fun `the speech prompt is a glossary capped to Whisper's window`() {
        assertEquals("Glossary: Grafana, Kubernetes, Tailscale, vLLM.", speechPrompt(vocabulary))
        assertNull(speechPrompt(emptyList()))
        val long = speechPrompt(List(500) { "term$it" })!!
        assertTrue(long.length <= 900)
    }

    @Test
    fun `a recited glossary is removed`() {
        assertEquals("", withoutRecitedGlossary(" Glossary: Grafana, Kubernetes, Tailscale, vLLM.", vocabulary))
        assertEquals("", withoutRecitedGlossary(" Grafana, Kubernetes, Tailscale, vLLM.", vocabulary))
        assertEquals(
            "Restart the pods. Thanks.",
            withoutRecitedGlossary("Restart the pods. Grafana, Kubernetes, Tailscale. Thanks.", vocabulary)
        )
    }

    @Test
    fun `dictated mentions of the words are kept`() {
        val spoken = " We moved Tailscale, Grafana, and Kubernetes to the new cluster."
        assertEquals(spoken, withoutRecitedGlossary(spoken, vocabulary))
        val short = " Grafana, Kubernetes look fine."
        assertEquals(short, withoutRecitedGlossary(short, vocabulary))
    }

    @Test
    fun `the cleanup prompt carries the vocabulary before the output rules`() {
        val prompt = SystemPromptBuilder.build(Profile(name = "T"), null, emptyList(), vocabulary)
        val listed = prompt.indexOf("<vocabulary>\nGrafana, Kubernetes, Tailscale, vLLM\n</vocabulary>")
        assertTrue(listed >= 0)
        assertTrue(listed < prompt.indexOf(SystemPromptBuilder.PRECEDENCE_HEADER))
        assertTrue(!SystemPromptBuilder.build(Profile(name = "T"), null, emptyList()).contains("<vocabulary>"))
    }
}
