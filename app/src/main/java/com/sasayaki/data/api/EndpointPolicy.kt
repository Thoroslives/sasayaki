package com.sasayaki.data.api

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object EndpointPolicy {
    fun normalizeBaseUrl(baseUrl: String): String {
        val trimmedBaseUrl = baseUrl.trim()
        require(trimmedBaseUrl.isNotEmpty()) { "Enter the server address." }

        // Request paths already start with v1/, but providers document their base URL
        // with it included, so a pasted ".../v1" would otherwise become ".../v1/v1/".
        val normalizedUrl = trimmedBaseUrl.trimEnd('/').removeSuffix("/v1") + "/"

        val parsedUrl = normalizedUrl.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("That address is not valid.")

        require(parsedUrl.isHttps) { "Use an https:// address. Unencrypted http is not allowed." }

        return parsedUrl.toString()
    }
}
