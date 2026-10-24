package com.sasayaki.domain.provider

import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ConnectionErrorsTest {
    private fun http(code: Int) = HttpException(Response.error<Unit>(code, "".toResponseBody()))

    @Test
    fun `network failures name the likely cause`() {
        assertEquals("Can't find that server. Check the address.", describeConnectionError(UnknownHostException("x")))
        assertEquals("The server took too long to answer.", describeConnectionError(SocketTimeoutException()))
    }

    @Test
    fun `auth failures point at the key`() {
        assertEquals("The server rejected the API key.", describeConnectionError(http(401)))
        assertEquals("The server rejected the API key.", describeConnectionError(http(403)))
    }

    @Test
    fun `other http errors keep the status code`() {
        assertEquals("The server had an error (502).", describeConnectionError(http(502)))
        assertEquals("The server answered with error 418.", describeConnectionError(http(418)))
    }

    @Test
    fun `invalid addresses surface the policy message`() {
        assertEquals(
            "Only HTTPS base URLs are allowed.",
            describeConnectionError(IllegalArgumentException("Only HTTPS base URLs are allowed."))
        )
    }
}
