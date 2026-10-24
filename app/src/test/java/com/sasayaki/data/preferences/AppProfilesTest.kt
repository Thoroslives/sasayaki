package com.sasayaki.data.preferences

import org.junit.Assert.assertEquals
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
}
