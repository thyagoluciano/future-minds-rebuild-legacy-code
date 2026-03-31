package br.com.zup.realwave.sales.manager.consumer.listener

import br.com.zup.realwave.sales.manager.api.response.CallbackResponse
import br.com.zup.realwave.sales.manager.api.response.PaymentResponse
import br.com.zup.realwave.sales.manager.api.response.PurchaseOrderResponse
import br.com.zup.realwave.sales.manager.consumer.service.CallbackService
import br.com.zup.realwave.sales.manager.infrastructure.kafka.KafkaEventContext
import br.com.zup.realwave.sales.manager.infrastructure.kafka.KafkaEventEnvelope
import br.com.zup.realwave.sales.manager.infrastructure.kafka.KafkaEventHeader
import br.com.zup.realwave.sales.manager.infrastructure.kafka.KafkaEventPayload
import br.com.zup.realwave.sales.manager.infrastructure.kafka.PurchaseOrderSnapshot
import br.com.zup.realwave.sales.manager.query.repository.PurchaseOrderQueryRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PurchaseOrderStatusConsumerTest {

    private lateinit var callbackService: CallbackService
    private lateinit var queryRepository: PurchaseOrderQueryRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var consumer: PurchaseOrderStatusConsumer

    @BeforeEach
    fun setup() {
        callbackService = mockk()
        queryRepository = mockk()
        objectMapper = mockk()
        consumer = PurchaseOrderStatusConsumer(callbackService, queryRepository, objectMapper)
    }

    @Test
    fun `should notify callback when purchase order has callback configured`() {
        val purchaseOrderId = "po-123"
        val tenantId = "tenant1"
        val callbackUrl = "https://example.com/callback"
        val envelope = buildEnvelope(purchaseOrderId, tenantId)
        val envelopeJson = """{"message": "test"}"""
        val purchaseOrder = buildPurchaseOrderResponse(purchaseOrderId, callbackUrl)

        every { objectMapper.readValue(envelopeJson, KafkaEventEnvelope::class.java) } returns envelope
        every { queryRepository.findById(purchaseOrderId) } returns purchaseOrder
        justRun { callbackService.notify(purchaseOrder.callback!!, envelope) }

        consumer.consume(envelopeJson)

        verify(exactly = 1) { callbackService.notify(purchaseOrder.callback!!, envelope) }
    }

    @Test
    fun `should not notify callback when purchase order has no callback configured`() {
        val purchaseOrderId = "po-456"
        val tenantId = "tenant1"
        val envelope = buildEnvelope(purchaseOrderId, tenantId)
        val envelopeJson = """{"message": "test"}"""
        val purchaseOrder = buildPurchaseOrderResponse(purchaseOrderId, callbackUrl = null)

        every { objectMapper.readValue(envelopeJson, KafkaEventEnvelope::class.java) } returns envelope
        every { queryRepository.findById(purchaseOrderId) } returns purchaseOrder

        consumer.consume(envelopeJson)

        verify(exactly = 0) { callbackService.notify(any(), any()) }
    }

    @Test
    fun `should not notify callback when purchase order is not found`() {
        val purchaseOrderId = "po-not-found"
        val tenantId = "tenant1"
        val envelope = buildEnvelope(purchaseOrderId, tenantId)
        val envelopeJson = """{"message": "test"}"""

        every { objectMapper.readValue(envelopeJson, KafkaEventEnvelope::class.java) } returns envelope
        every { queryRepository.findById(purchaseOrderId) } returns null

        consumer.consume(envelopeJson)

        verify(exactly = 0) { callbackService.notify(any(), any()) }
    }

    @Test
    fun `should not rethrow exception when processing fails`() {
        val envelopeJson = """{"invalid": "json"}"""

        every { objectMapper.readValue(envelopeJson, KafkaEventEnvelope::class.java) } throws RuntimeException("parse error")

        // Consumer catches and logs — should NOT throw
        consumer.consume(envelopeJson)

        verify(exactly = 0) { callbackService.notify(any(), any()) }
    }

    private fun buildEnvelope(purchaseOrderId: String, tenantId: String): KafkaEventEnvelope {
        val snapshot = PurchaseOrderSnapshot(
            id = purchaseOrderId,
            status = "OPENED",
            type = null,
            customer = null,
            items = emptyList(),
            payment = br.com.zup.realwave.sales.manager.domain.Payment(methods = mutableListOf(), description = null),
            freight = null,
            coupon = null,
            customerOrder = null,
            protocol = null,
            subscriptionId = null,
            segmentation = null,
            onBoardingSale = null,
            mgm = null,
            salesForce = null,
            createdAt = null,
            updatedAt = null
        )
        return KafkaEventEnvelope(
            header = KafkaEventHeader(
                eventId = "event-id-001",
                eventType = "PurchaseOrderCreated",
                timestamp = "2024-01-01T00:00:00",
                context = KafkaEventContext(tenantId = tenantId)
            ),
            payload = KafkaEventPayload(purchaseOrder = snapshot)
        )
    }

    private fun buildPurchaseOrderResponse(id: String, callbackUrl: String?): PurchaseOrderResponse {
        val callback = callbackUrl?.let { CallbackResponse(url = it, headers = null) }
        return PurchaseOrderResponse(
            id = id,
            type = "ACQUISITION",
            protocol = null,
            subscriptionId = null,
            segmentation = null,
            mgm = null,
            salesForce = null,
            onBoardingSale = null,
            customer = null,
            coupon = null,
            totalPrice = null,
            discount = null,
            payment = PaymentResponse(methods = emptyList(), description = null),
            freight = null,
            status = "OPENED",
            items = emptyList(),
            installationAttributes = emptyList(),
            channelCreate = null,
            channelCheckout = null,
            callback = callback,
            reason = null,
            createdAt = "2024-01-01T00:00:00",
            updatedAt = "2024-01-01T00:00:00"
        )
    }
}
