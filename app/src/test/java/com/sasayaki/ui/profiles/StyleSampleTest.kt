package com.sasayaki.ui.profiles

import com.sasayaki.domain.model.OutputStyle
import com.sasayaki.domain.model.RewriteMode
import com.sasayaki.domain.model.SummarizeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StyleSampleTest {

    @Test
    fun `every combination has a sample`() {
        OutputStyle.entries.forEach { style ->
            RewriteMode.entries.forEach { rewrite ->
                SummarizeMode.entries.forEach { summarize ->
                    assertTrue(styleSample(style, rewrite, summarize, emoji = false).isNotBlank())
                }
            }
        }
    }

    @Test
    fun `punctuation styles follow the output rules`() {
        val full = styleSample(OutputStyle.STANDARD, RewriteMode.FIX, SummarizeMode.LIGHT, emoji = false)
        val light = styleSample(OutputStyle.RELAXED, RewriteMode.FIX, SummarizeMode.LIGHT, emoji = false)
        val minimal = styleSample(OutputStyle.MINIMAL, RewriteMode.FIX, SummarizeMode.LIGHT, emoji = false)
        assertTrue(full.endsWith(".") && full.contains(","))
        assertFalse(light.endsWith(".") || light.contains(","))
        assertEquals(minimal.lowercase(), minimal)
        assertTrue(minimal.none { it in ".,?!" })
    }

    @Test
    fun `emoji is added only to casual rewrites`() {
        assertTrue(styleSample(OutputStyle.STANDARD, RewriteMode.FIX, SummarizeMode.NONE, emoji = true).endsWith("🍜"))
        assertFalse(styleSample(OutputStyle.STANDARD, RewriteMode.POLISH, SummarizeMode.NONE, emoji = true).endsWith("🍜"))
    }
}
