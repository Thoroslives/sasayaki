package com.sasayaki.domain.provider

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private val knownHosts = mapOf(
    "api.openai.com" to "OpenAI",
    "api.groq.com" to "Groq",
    "openrouter.ai" to "OpenRouter",
    "api.mistral.ai" to "Mistral",
    "nano-gpt.com" to "NanoGPT",
    "api.ppq.ai" to "PPQ",
    "ppq.ai" to "PPQ",
    "inference.tinfoil.sh" to "Tinfoil",
    "api.deepinfra.com" to "DeepInfra",
    "api.together.xyz" to "Together"
)

/** suggestProviderName returns a readable default name for [baseUrl], or null if the address is unusable. */
fun suggestProviderName(baseUrl: String): String? {
    val host = baseUrl.trim().toHttpUrlOrNull()?.host ?: return null
    knownHosts[host]?.let { return it }
    if (host.all { it.isDigit() || it == '.' } || host == "localhost") return host
    val label = host.removePrefix("api.").removePrefix("www.").substringBefore('.')
    return label.replaceFirstChar { it.uppercase() }
}
