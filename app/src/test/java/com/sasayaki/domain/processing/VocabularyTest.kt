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
        val many = List(500) { "term$it" }
        assertTrue(speechPrompt(many)!!.length <= 540)
        val reach = vocabularyReach(many)
        assertTrue(reach.speech < reach.cleanup && reach.cleanup < many.size)
    }

    @Test
    fun `a recited glossary leading the transcript is removed`() {
        assertEquals("", withoutRecitedGlossary(" Glossary: Grafana, Kubernetes, Tailscale, vLLM.", vocabulary))
        assertEquals("", withoutRecitedGlossary(" Grafana, Kubernetes, Tailscale, vLLM.", vocabulary))
        assertEquals("Restart the pods.", withoutRecitedGlossary("Glossary: Grafana, Kubernetes. Restart the pods.", vocabulary))
        val dotted = sortedVocabulary(listOf("Kotlin", "Node.js", "Postgres", "React"))
        assertEquals("", withoutRecitedGlossary("Glossary: Kotlin, Node.js, Postgres, React.", dotted))
    }

    @Test
    fun `dictated mentions of the words are kept`() {
        listOf(
            " We moved Tailscale, Grafana, and Kubernetes to the new cluster.",
            " Grafana, Kubernetes look fine.",
            "Restart the pods. Grafana, Kubernetes, Tailscale. Thanks."
        ).forEach { assertEquals(it, withoutRecitedGlossary(it, vocabulary)) }
        val people = sortedVocabulary(listOf("Alice", "Bob", "Carol", "Dave"))
        val invite = "Invite Alice, Bob, Carol, and Dave to the meeting."
        assertEquals(invite, withoutRecitedGlossary(invite, people))
    }

    @Test
    fun `without a vocabulary nothing is treated as a glossary`() {
        val spoken = "Add this to the glossary: Kubernetes is a container orchestrator. Thanks."
        assertEquals(spoken, withoutRecitedGlossary(spoken, emptyList()))
    }

    @Test
    fun `overlong terms are reported`() {
        assertTrue(hasOverlongTerm("short, " + "x".repeat(MAX_VOCABULARY_TERM_LENGTH + 1)))
        assertTrue(!hasOverlongTerm("short, fine"))
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
