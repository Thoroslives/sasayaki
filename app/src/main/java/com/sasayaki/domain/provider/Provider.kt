package com.sasayaki.domain.provider

import androidx.compose.runtime.Immutable

enum class Purpose { SPEECH, CLEANUP }

@Immutable
data class Provider(
    val id: Long = 0,
    val name: String,
    val baseUrl: String,
    val apiKey: String = "",
    val speechModel: String? = null,
    val cleanupModel: String? = null
) {
    fun defaultModel(purpose: Purpose): String? = when (purpose) {
        Purpose.SPEECH -> speechModel
        Purpose.CLEANUP -> cleanupModel
    }?.takeIf { it.isNotBlank() }

    // Keys stay out of logs and crash reports.
    override fun toString() = "Provider(id=$id, name=$name, baseUrl=$baseUrl, speechModel=$speechModel, cleanupModel=$cleanupModel)"
}

@Immutable
data class Endpoint(
    val providerId: Long,
    val providerName: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String
) {
    override fun toString() = "Endpoint(providerId=$providerId, baseUrl=$baseUrl, model=$model)"
}

sealed interface Resolution {
    data class Ready(val endpoint: Endpoint) : Resolution
    data object NoProvider : Resolution
    data class NoModel(val provider: Provider) : Resolution
}

/**
 * Picks the provider and model a profile uses for [purpose]: the profile's chosen
 * provider if it still exists, otherwise the first provider with a default model for
 * that purpose; then the profile's model override, otherwise the provider default.
 */
fun resolveEndpoint(
    providers: List<Provider>,
    purpose: Purpose,
    chosenProviderId: Long?,
    modelOverride: String
): Resolution {
    val provider = chosenProviderId?.let { id -> providers.firstOrNull { it.id == id } }
        ?: providers.firstOrNull { it.defaultModel(purpose) != null }
        ?: return Resolution.NoProvider
    val model = modelOverride.trim().ifEmpty { provider.defaultModel(purpose).orEmpty() }
    if (model.isEmpty()) return Resolution.NoModel(provider)
    return Resolution.Ready(Endpoint(provider.id, provider.name, provider.baseUrl, provider.apiKey, model))
}
