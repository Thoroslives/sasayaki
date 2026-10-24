package com.sasayaki.domain.provider

import com.sasayaki.data.api.EndpointPolicy

/** Returns why [url] cannot be used, or null when it is usable or still empty. */
fun serverAddressError(url: String): String? {
    if (url.isBlank()) return null
    return runCatching { EndpointPolicy.normalizeBaseUrl(url) }.exceptionOrNull()?.message
}
