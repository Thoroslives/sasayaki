package com.sasayaki.domain.provider

data class LegacyEndpoint(val baseUrl: String, val apiKey: String, val model: String)

data class LegacyMigrationPlan(val providers: List<Provider>, val speechProviderIndex: Int?, val cleanupProviderIndex: Int?)

/**
 * planLegacyMigration turns the old single speech and cleanup endpoints into providers. One server used
 * for both becomes one provider with both default models.
 */
fun planLegacyMigration(speech: LegacyEndpoint?, cleanup: LegacyEndpoint?): LegacyMigrationPlan {
    val speechUsable = speech?.takeIf { it.baseUrl.isNotBlank() }
    val cleanupUsable = cleanup?.takeIf { it.baseUrl.isNotBlank() }

    fun provider(endpoint: LegacyEndpoint, speechModel: String?, cleanupModel: String?) = Provider(
        name = suggestProviderName(endpoint.baseUrl) ?: "Server",
        baseUrl = endpoint.baseUrl.trim(),
        apiKey = endpoint.apiKey.trim(),
        speechModel = speechModel?.trim()?.ifEmpty { null },
        cleanupModel = cleanupModel?.trim()?.ifEmpty { null }
    )

    if (speechUsable != null && cleanupUsable != null && sameServer(speechUsable, cleanupUsable)) {
        return LegacyMigrationPlan(listOf(provider(speechUsable, speechUsable.model, cleanupUsable.model)), 0, 0)
    }
    val providers = listOfNotNull(
        speechUsable?.let { provider(it, it.model, null) },
        cleanupUsable?.let { provider(it, null, it.model) }
    )
    return LegacyMigrationPlan(
        providers = providers,
        speechProviderIndex = if (speechUsable != null) 0 else null,
        cleanupProviderIndex = if (cleanupUsable != null) providers.lastIndex else null
    )
}

private fun sameServer(a: LegacyEndpoint, b: LegacyEndpoint): Boolean =
    sameServer(a.baseUrl, b.baseUrl) && a.apiKey.trim() == b.apiKey.trim()

fun sameServer(a: String, b: String): Boolean = a.serverRoot() == b.serverRoot()

private fun String.serverRoot() = trim().trimEnd('/').removeSuffix("/v1")
