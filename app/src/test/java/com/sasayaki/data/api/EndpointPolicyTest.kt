package com.sasayaki.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EndpointPolicyTest {
    @Test
    fun `server root gains a trailing slash`() {
        assertEquals("https://api.example.com/", EndpointPolicy.normalizeBaseUrl("https://api.example.com"))
    }

    @Test
    fun `documented v1 suffix is dropped so paths are not doubled`() {
        assertEquals("https://api.example.com/", EndpointPolicy.normalizeBaseUrl("https://api.example.com/v1"))
        assertEquals("https://api.example.com/", EndpointPolicy.normalizeBaseUrl(" https://api.example.com/v1/ "))
        assertEquals("https://api.example.com/openai/", EndpointPolicy.normalizeBaseUrl("https://api.example.com/openai/v1"))
    }

    @Test
    fun `paths that only contain v1 are kept`() {
        assertEquals("https://example.com/v1beta/", EndpointPolicy.normalizeBaseUrl("https://example.com/v1beta"))
    }

    @Test
    fun `cleartext and malformed urls are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { EndpointPolicy.normalizeBaseUrl("http://example.com") }
        assertThrows(IllegalArgumentException::class.java) { EndpointPolicy.normalizeBaseUrl("not a url") }
        assertThrows(IllegalArgumentException::class.java) { EndpointPolicy.normalizeBaseUrl("  ") }
    }
}
