package com.sasayaki.domain.processing

import com.sasayaki.domain.model.Profile
import com.sasayaki.domain.model.SummarizeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CleanupOutputTest {

    private val input = "so um I think we should meet tomorrow at five"
    private val cleaned = "I think we should meet tomorrow at five."

    @Test
    fun `reasoning blocks are removed`() {
        assertEquals(cleaned, stripModelArtifacts("<think>The user wants a cleanup.</think>\n$cleaned", input))
        assertEquals(cleaned, stripModelArtifacts("<THINKING>\nhmm\n</THINKING>$cleaned", input))
    }

    @Test
    fun `a reasoning block opened by the template is cut at its end`() {
        assertEquals(cleaned, stripModelArtifacts("Remove the filler first.\n</think>\n\n$cleaned", input))
    }

    @Test
    fun `reasoning cut off by the token limit leaves nothing`() {
        assertEquals("", stripModelArtifacts("<think>First I will remove the filler and then", input))
    }

    @Test
    fun `framing labels, tags, fences and notes are removed`() {
        assertEquals(cleaned, stripModelArtifacts("Here is the cleaned text:\n$cleaned", input))
        assertEquals(cleaned, stripModelArtifacts("Here's the corrected version: $cleaned", input))
        assertEquals(cleaned, stripModelArtifacts("Cleaned transcript: $cleaned", input))
        assertEquals(cleaned, stripModelArtifacts("<transcript>\n$cleaned\n</transcript>", input))
        assertEquals(cleaned, stripModelArtifacts("```\n$cleaned\n```", input))
        assertEquals(cleaned, stripModelArtifacts("$cleaned\n\n(Note: I removed the filler words.)", input))
    }

    @Test
    fun `wrapping quotes are removed unless dictated`() {
        assertEquals(cleaned, stripModelArtifacts("\"$cleaned\"", input))
        assertEquals(cleaned, stripModelArtifacts("“$cleaned”", input))
        assertEquals("\"Fine,\" she said.", stripModelArtifacts("\"Fine,\" she said.", "\"fine\" she said"))
        assertEquals("\"Stop.\"", stripModelArtifacts("\"Stop.\"", "\"stop\""))
        assertEquals("\"Yes\" and \"no\"", stripModelArtifacts("\"Yes\" and \"no\"", "yes and no"))
    }

    @Test
    fun `framing the speaker dictated is kept`() {
        val plan = "Here's the plan: we meet at five."
        assertEquals(plan, stripModelArtifacts(plan, "here's the plan we meet at five"))
        val label = "Here is the final version: done."
        assertEquals(label, stripModelArtifacts(label, "here is the final version: done"))
        val note = "Buy milk.\nNote: the shop closes at six."
        assertEquals(note, stripModelArtifacts(note, "buy milk note the shop closes at six"))
    }

    @Test
    fun `dictated sections, spoken labels and spoken quotes survive`() {
        val changes = "Fixed the login bug.\n\nChanges:\n- updated the token refresh\n- added tests"
        assertEquals(changes, stripModelArtifacts(changes, "fixed the login bug new paragraph changes colon"))
        val label = "Final version: the budget is approved."
        assertEquals(label, stripModelArtifacts(label, "final version colon the budget is approved"))
        assertEquals("\"Stop.\"", stripModelArtifacts("\"Stop.\"", "quote stop unquote"))
        assertEquals("“Stop.”", stripModelArtifacts("“Stop.”", "\"stop\""))
    }

    @Test
    fun `gpt-oss channels are reduced to the final answer`() {
        val raw = "<|channel|>analysis<|message|>Remove filler.<|end|><|start|>assistant<|channel|>final<|message|>$cleaned<|return|>"
        assertEquals(cleaned, stripModelArtifacts(raw, input))
        assertEquals("", stripModelArtifacts("<|channel|>analysis<|message|>Remove the filler and", input))
    }

    @Test
    fun `nested code fences are left alone`() {
        val fenced = "```\nfirst\n```\nbetween\n```\nsecond\n```"
        assertEquals(fenced, stripModelArtifacts(fenced, "first between second"))
    }

    @Test
    fun `untouched output keeps dictated line breaks`() {
        assertEquals("\nThanks for the update.", stripModelArtifacts("\nThanks for the update.", "new paragraph thanks for the update"))
        assertEquals("Thanks.\n", stripModelArtifacts(" Thanks.\n", "thanks new line"))
    }

    @Test
    fun `custom prompts lift the length checks`() {
        val request = "translate this into japanese please"
        val long = "x".repeat(200)
        assertNull(rejectCleanup(request, long, "stop", lengthCheck = LengthCheck.NONE))
        assertEquals(LengthCheck.NONE, lengthCheckFor(Profile(name = "T", profilePrompt = "Translate to English")))
        assertEquals(LengthCheck.NONE, lengthCheckFor(Profile(name = "T", selectedPromptIds = setOf(4L))))
        assertEquals(LengthCheck.GROWTH_ONLY, lengthCheckFor(Profile(name = "T", summarizeMode = SummarizeMode.LIGHT)))
        assertEquals(LengthCheck.FULL, lengthCheckFor(Profile(name = "T")))
    }

    @Test
    fun `a sound cleanup is accepted`() {
        assertNull(rejectCleanup(input, cleaned, "stop", lengthCheck = LengthCheck.FULL))
        assertNull(rejectCleanup(input, cleaned, null, lengthCheck = LengthCheck.FULL))
    }

    @Test
    fun `failures are rejected`() {
        assertEquals(CleanupRejection.TRUNCATED, rejectCleanup(input, cleaned, "length", lengthCheck = LengthCheck.FULL))
        assertEquals(CleanupRejection.EMPTY, rejectCleanup(input, "  ", "stop", lengthCheck = LengthCheck.FULL))
        assertEquals(CleanupRejection.REPEATED, rejectCleanup(input, "$cleaned $cleaned", "stop", lengthCheck = LengthCheck.FULL))
        assertEquals(CleanupRejection.REPEATED, rejectCleanup(input, "$input\n\n$input", "stop", lengthCheck = LengthCheck.FULL))
    }

    @Test
    fun `an answer to the dictation is rejected`() {
        val request = "write me an email to John saying I'm sick today"
        val answer = "Subject: Sick Day\n\nHi John,\n\n" + "I wanted to let you know that I'm not feeling well. ".repeat(4) +
            "\n\nBest regards"
        assertEquals(CleanupRejection.GREW, rejectCleanup(request, answer, "stop", lengthCheck = LengthCheck.FULL))
    }

    @Test
    fun `heavy shrinking is rejected only when not condensing`() {
        val long = "I went to the store and I bought apples and pears and then I walked home along the river " +
            "and I met my neighbour who told me about her new job at the hospital downtown"
        val stub = "I went shopping."
        assertEquals(CleanupRejection.SHRANK, rejectCleanup(long, stub, "stop", lengthCheck = LengthCheck.FULL))
        assertNull(rejectCleanup(long, stub, "stop", lengthCheck = LengthCheck.GROWTH_ONLY))
    }
}
