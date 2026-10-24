package com.sasayaki.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DictationFormatTest {
    @Test
    fun `plain app label is used as is`() {
        assertEquals("Notes", displaySourceApp("Notes", "com.example.notes"))
    }

    @Test
    fun `generic label falls back to the package`() {
        assertEquals("WhatsApp", displaySourceApp("App", "com.whatsapp"))
    }

    @Test
    fun `package used as label is resolved`() {
        assertEquals("Signal", displaySourceApp("org.thoughtcrime.securesms.signal", null))
    }

    @Test
    fun `unknown package skips generic segments`() {
        assertEquals("Fastmail", displaySourceApp(null, "com.fastmail.app"))
    }

    @Test
    fun `missing source yields no label`() {
        assertNull(displaySourceApp(null, null))
        assertNull(displaySourceApp("  ", ""))
    }

    @Test
    fun `durations pick the two largest units`() {
        assertEquals("12s", formatDuration(12_400))
        assertEquals("4m 47s", formatDuration(287_000))
        assertEquals("1h 10m", formatDuration(4_200_000))
    }

    @Test
    fun `time saved compares against typing and never goes negative`() {
        assertEquals(9, minutesSaved(words = 400, speakingMs = 60_000))
        assertEquals(0, minutesSaved(words = 10, speakingMs = 120_000))
    }

    @Test
    fun `pace needs words and at least a second of speech`() {
        assertEquals(150, wordsPerMinute(words = 150, speakingMs = 60_000))
        assertNull(wordsPerMinute(words = 0, speakingMs = 60_000))
        assertNull(wordsPerMinute(words = 5, speakingMs = 500))
    }

    @Test
    fun `large numbers are compacted`() {
        assertEquals("9999", formatCompactNumber(9_999))
        assertEquals("12.4K", formatCompactNumber(12_400))
        assertEquals("1.5M", formatCompactNumber(1_500_000))
    }
}
