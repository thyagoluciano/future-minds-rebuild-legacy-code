package br.com.zup.realwave.sales.manager.consumer.service

import br.com.zup.realwave.sales.manager.api.response.CallbackResponse
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

class CallbackServiceTest {

    private lateinit var restTemplate: RestTemplate
    private lateinit var callbackService: CallbackService

    @BeforeEach
    fun setup() {
        restTemplate = mockk()
        callbackService = CallbackService(restTemplate)
    }

    @Test
    fun `should call RestTemplate postForEntity with callback url and payload`() {
        val callbackUrl = "https://example.com/callback"
        val callback = CallbackResponse(url = callbackUrl, headers = null)
        val payload = mapOf("key" to "value")
        val httpEntitySlot = slot<HttpEntity<*>>()

        every {
            restTemplate.postForEntity(
                callbackUrl,
                capture(httpEntitySlot),
                String::class.java
            )
        } returns ResponseEntity("OK", HttpStatus.OK)

        callbackService.notify(callback, payload)

        verify(exactly = 1) {
            restTemplate.postForEntity(callbackUrl, any<HttpEntity<*>>(), String::class.java)
        }
        assertEquals(payload, httpEntitySlot.captured.body)
    }

    @Test
    fun `should add custom headers when callback has headers configured`() {
        val callbackUrl = "https://example.com/callback"
        val headersNode = JsonNodeFactory.instance.objectNode().apply {
            put("Authorization", "Bearer token-123")
            put("X-Custom-Header", "custom-value")
        }
        val callback = CallbackResponse(url = callbackUrl, headers = headersNode)
        val payload = mapOf("event" to "PurchaseOrderCreated")
        val httpEntitySlot = slot<HttpEntity<*>>()

        every {
            restTemplate.postForEntity(
                callbackUrl,
                capture(httpEntitySlot),
                String::class.java
            )
        } returns ResponseEntity("OK", HttpStatus.OK)

        callbackService.notify(callback, payload)

        verify(exactly = 1) {
            restTemplate.postForEntity(callbackUrl, any<HttpEntity<*>>(), String::class.java)
        }
        val capturedHeaders = httpEntitySlot.captured.headers
        assertEquals("Bearer token-123", capturedHeaders.getFirst("Authorization"))
        assertEquals("custom-value", capturedHeaders.getFirst("X-Custom-Header"))
    }

    @Test
    fun `should not throw when RestTemplate throws exception`() {
        val callbackUrl = "https://example.com/callback"
        val callback = CallbackResponse(url = callbackUrl, headers = null)
        val payload = mapOf("event" to "PurchaseOrderCreated")

        every {
            restTemplate.postForEntity(callbackUrl, any<HttpEntity<*>>(), String::class.java)
        } throws RuntimeException("Connection refused")

        // CallbackService swallows the exception and logs it
        callbackService.notify(callback, payload)

        verify(exactly = 1) {
            restTemplate.postForEntity(callbackUrl, any<HttpEntity<*>>(), String::class.java)
        }
    }

    @Test
    fun `should use application json content type in request`() {
        val callbackUrl = "https://example.com/callback"
        val callback = CallbackResponse(url = callbackUrl, headers = null)
        val payload = "test-payload"
        val httpEntitySlot = slot<HttpEntity<*>>()

        every {
            restTemplate.postForEntity(
                callbackUrl,
                capture(httpEntitySlot),
                String::class.java
            )
        } returns ResponseEntity("OK", HttpStatus.OK)

        callbackService.notify(callback, payload)

        val contentType = httpEntitySlot.captured.headers.contentType
        assertEquals("application/json", contentType?.toString())
    }
}
