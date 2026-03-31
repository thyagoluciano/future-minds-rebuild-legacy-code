package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CallbackTest {

    @Test
    fun `should create Callback with url and empty headers by default`() {
        val callback = Callback(url = "https://example.com/callback")
        assertEquals("https://example.com/callback", callback.url)
        assertTrue(callback.headers.isEmpty())
    }

    @Test
    fun `should create Callback with headers`() {
        val headers = mapOf("Authorization" to "Bearer token")
        val callback = Callback(url = "https://example.com/callback", headers = headers)
        assertEquals(headers, callback.headers)
    }

    @Test
    fun `two Callbacks with same data should be equal`() {
        val c1 = Callback("https://example.com")
        val c2 = Callback("https://example.com")
        assertEquals(c1, c2)
    }

    @Test
    fun `should throw exception when url is blank`() {
        assertThrows<IllegalArgumentException> { Callback("") }
    }
}
