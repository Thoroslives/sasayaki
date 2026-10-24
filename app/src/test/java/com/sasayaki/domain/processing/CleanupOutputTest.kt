package com.sasayaki.domain.processing

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
    fun `a sound cleanup is accepted`() {
        assertNull(rejectCleanup(input, cleaned, "stop", condensing = false))
        assertNull(rejectCleanup(input, cleaned, null, condensing = false))
    }

    @Test
    fun `failures are rejected`() {
        assertEquals(CleanupRejection.TRUNCATED, rejectCleanup(input, cleaned, "length", condensing = false))
        assertEquals(CleanupRejection.EMPTY, rejectCleanup(input, "  ", "stop", condensing = false))
        assertEquals(CleanupRejection.REPEATED, rejectCleanup(input, "$cleaned $cleaned", "stop", condensing = false))
        assertEquals(CleanupRejection.REPEATED, rejectCleanup(input, "$input\n\n$input", "stop", condensing = false))
    }

    @Test
    fun `an answer to the dictation is rejected`() {
        val request = "write me an email to John saying I'm sick today"
        val answer = "Subject: Sick Day\n\nHi John,\n\n" + "I wanted to let you know that I'm not feeling well. ".repeat(4) +
            "\n\nBest regards"
        assertEquals(CleanupRejection.GREW, rejectCleanup(request, answer, "stop", condensing = false))
    }

    @Test
    fun `heavy shrinking is rejected only when not condensing`() {
        val long = "I went to the store and I bought apples and pears and then I walked home along the river " +
            "and I met my neighbour who told me about her new job at the hospital downtown"
        val stub = "I went shopping."
        assertEquals(CleanupRejection.SHRANK, rejectCleanup(long, stub, "stop", condensing = false))
        assertNull(rejectCleanup(long, stub, "stop", condensing = true))
    }
}
