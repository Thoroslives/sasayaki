package com.sasayaki.domain.processing

import com.sasayaki.domain.model.AppContext
import com.sasayaki.domain.model.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AppCategoryTest {

    @Test
    fun `known packages map to their category`() {
        assertEquals(AppCategory.EMAIL, appCategoryOf("com.google.android.gm"))
        assertEquals(AppCategory.CHAT, appCategoryOf("org.thoughtcrime.securesms"))
        assertEquals(AppCategory.NOTES, appCategoryOf("md.obsidian"))
        assertEquals(AppCategory.SOCIAL, appCategoryOf("com.twitter.android"))
        assertEquals(AppCategory.AI_CHAT, appCategoryOf("com.openai.chatgpt"))
    }

    @Test
    fun `packages that merely contain a keyword are not matched`() {
        listOf("org.mozilla.firefox", "com.termux", "com.example.mailtracker", "app.notesy.x").forEach {
            assertNull("$it must not be categorised", appCategoryOf(it))
        }
        assertNull(appCategoryOf(null))
    }

    @Test
    fun `unknown apps get no style hint`() {
        val prompt = SystemPromptBuilder.build(
            Profile(name = "Test"),
            AppContext(label = "Firefox", packageName = "org.mozilla.firefox"),
            emptyList()
        )
        assertFalse(prompt.contains("App style hint:"))
    }

    @Test
    fun `ai chats get no style hint`() {
        val prompt = SystemPromptBuilder.build(
            Profile(name = "Test"),
            AppContext(label = "ChatGPT", packageName = "com.openai.chatgpt"),
            emptyList()
        )
        assertFalse(prompt.contains("App style hint:"))
    }
}
