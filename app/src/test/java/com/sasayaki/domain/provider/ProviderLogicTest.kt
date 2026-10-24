package com.sasayaki.domain.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderLogicTest {
    private val speaches = Provider(id = 1, name = "Home", baseUrl = "https://asr.home", speechModel = "large-v3-turbo")
    private val llama = Provider(id = 2, name = "llama", baseUrl = "https://llm.home", cleanupModel = "qwen")
    private val openai =
        Provider(
            id = 3,
            name = "OpenAI",
            baseUrl = "https://api.openai.com",
            apiKey = "k",
            speechModel = "whisper-1",
            cleanupModel = "gpt-4o-mini"
        )

    @Test
    fun `chosen provider and its default model are used`() {
        val result = resolveEndpoint(listOf(speaches, openai), Purpose.SPEECH, chosenProviderId = 3, modelOverride = "")
        assertEquals(Endpoint(3, "OpenAI", "https://api.openai.com", "k", "whisper-1"), (result as Resolution.Ready).endpoint)
    }

    @Test
    fun `model override wins over the provider default`() {
        val result = resolveEndpoint(listOf(openai), Purpose.CLEANUP, chosenProviderId = 3, modelOverride = " gpt-5 ")
        assertEquals("gpt-5", (result as Resolution.Ready).endpoint.model)
    }

    @Test
    fun `without a choice the first provider offering the purpose is used`() {
        val result = resolveEndpoint(listOf(speaches, llama), Purpose.CLEANUP, chosenProviderId = null, modelOverride = "")
        assertEquals(2L, (result as Resolution.Ready).endpoint.providerId)
    }

    @Test
    fun `a deleted choice falls back instead of failing`() {
        val result = resolveEndpoint(listOf(speaches), Purpose.SPEECH, chosenProviderId = 99, modelOverride = "")
        assertEquals(1L, (result as Resolution.Ready).endpoint.providerId)
    }

    @Test
    fun `missing provider or model is reported`() {
        assertEquals(Resolution.NoProvider, resolveEndpoint(listOf(speaches), Purpose.CLEANUP, null, ""))
        assertEquals(Resolution.NoModel(speaches), resolveEndpoint(listOf(speaches), Purpose.CLEANUP, 1, ""))
    }

    @Test
    fun `models are classified from server hints and names`() {
        assertEquals(ModelKind.SPEECH, classifyModel("Systran/faster-whisper-large-v3"))
        assertEquals(ModelKind.SPEECH, classifyModel("gpt-4o-mini-transcribe"))
        assertEquals(ModelKind.SPEECH, classifyModel("nvidia/parakeet-tdt-0.6b-v2"))
        assertEquals(ModelKind.SPEECH, classifyModel("custom-model", task = "automatic-speech-recognition"))
        assertEquals(ModelKind.OTHER, classifyModel("tts-1"))
        assertEquals(ModelKind.OTHER, classifyModel("text-embedding-3-small"))
        assertEquals(ModelKind.CHAT, classifyModel("qwen3.5-2b-instruct"))
    }

    @Test
    fun `filtering keeps every model when none look suitable`() {
        val models = listOf(ModelInfo("qwen", ModelKind.CHAT), ModelInfo("tts-1", ModelKind.OTHER))
        assertEquals(listOf("qwen"), models.suitableFor(Purpose.CLEANUP))
        assertEquals(listOf("qwen", "tts-1"), models.suitableFor(Purpose.SPEECH))
    }

    @Test
    fun `provider names come from known hosts or the domain`() {
        assertEquals("OpenAI", suggestProviderName("https://api.openai.com/v1"))
        assertEquals("Example", suggestProviderName("https://api.example.org"))
        assertEquals("192.168.1.20", suggestProviderName("https://192.168.1.20:8000"))
        assertNull(suggestProviderName("not a url"))
    }

    @Test
    fun `one server for both purposes becomes one provider`() {
        val plan = planLegacyMigration(
            LegacyEndpoint("https://api.openai.com/", "k", "whisper-1"),
            LegacyEndpoint("https://api.openai.com/v1", "k", "gpt-4o-mini")
        )
        assertEquals(1, plan.providers.size)
        assertEquals("whisper-1", plan.providers.single().speechModel)
        assertEquals("gpt-4o-mini", plan.providers.single().cleanupModel)
        assertEquals(0, plan.speechProviderIndex)
        assertEquals(0, plan.cleanupProviderIndex)
    }

    @Test
    fun `separate servers become separate providers`() {
        val plan = planLegacyMigration(
            LegacyEndpoint("https://asr.home", "", "large-v3"),
            LegacyEndpoint("https://llm.home", "", "qwen")
        )
        assertEquals(listOf("large-v3", null), plan.providers.map { it.speechModel })
        assertEquals(listOf(null, "qwen"), plan.providers.map { it.cleanupModel })
        assertEquals(1, plan.cleanupProviderIndex)
    }

    @Test
    fun `unconfigured endpoints are skipped`() {
        val plan = planLegacyMigration(LegacyEndpoint("", "", "whisper-1"), LegacyEndpoint("https://llm.home", "", "qwen"))
        assertEquals(1, plan.providers.size)
        assertNull(plan.speechProviderIndex)
        assertEquals(0, plan.cleanupProviderIndex)
        assertTrue(planLegacyMigration(null, null).providers.isEmpty())
    }

    @Test
    fun `server comparison ignores a v1 suffix and trailing slashes`() {
        assertTrue(sameServer("https://api.example.com/v1/", " https://api.example.com"))
        assertTrue(!sameServer("https://a.example.com", "https://b.example.com"))
    }
}
