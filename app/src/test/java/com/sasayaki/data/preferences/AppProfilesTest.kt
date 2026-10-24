package com.sasayaki.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppProfilesTest {

    @Test
    fun `app profiles survive a round trip`() {
        val profiles = mapOf("org.thoughtcrime.securesms" to 3L, "com.google.android.gm" to 12L)
        assertEquals(profiles, decodeAppProfiles(encodeAppProfiles(profiles)))
    }

    @Test
    fun `malformed entries are skipped`() {
        val decoded = decodeAppProfiles(setOf("com.slack=4", "no-separator", "=5", "com.discord=abc"))
        assertEquals(mapOf("com.slack" to 4L), decoded)
    }

    @Test
    fun `a remembered profile applies only while remembering is on`() {
        val prefs = UserPreferences(appProfiles = mapOf("com.slack" to 4L))
        assertEquals(4L, prefs.rememberedProfileFor("com.slack"))
        assertNull(prefs.rememberedProfileFor("com.discord"))
        assertNull(prefs.rememberedProfileFor(null))
        assertNull(prefs.copy(rememberProfilePerApp = false).rememberedProfileFor("com.slack"))
    }
}
