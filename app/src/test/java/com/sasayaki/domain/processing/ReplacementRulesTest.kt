package com.sasayaki.domain.processing

import com.sasayaki.domain.model.TextReplacementRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ReplacementRulesTest {
    private fun rule(pattern: String, replacement: String, regex: Boolean = false) =
        TextReplacementRule(name = "", pattern = pattern, replacement = replacement, isRegex = regex)

    @Test
    fun `plain rules replace every occurrence in order`() {
        val rules = listOf(rule("sass", "Sasa"), rule("Sasa yaki", "Sasayaki"))
        assertEquals("Sasayaki and Sasayaki", applyReplacementRules("sass yaki and sass yaki", rules))
    }

    @Test
    fun `regex rules support group references`() {
        val rules = listOf(rule("""(\d+) percent""", "$1%", regex = true))
        assertEquals("up 12% today", applyReplacementRules("up 12 percent today", rules))
    }

    @Test
    fun `blank and invalid patterns leave the text alone`() {
        val rules = listOf(rule("", "x"), rule("(unclosed", "x", regex = true))
        assertEquals("unchanged", applyReplacementRules("unchanged", rules))
    }

    @Test
    fun `only invalid regex patterns report an error`() {
        assertNotNull(rule("(unclosed", "", regex = true).patternError())
        assertNull(rule("(closed)", "", regex = true).patternError())
        assertNull(rule("(unclosed", "", regex = false).patternError())
    }
}
