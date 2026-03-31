package br.com.zup.realwave.sales.manager.query.consumer

import br.com.zup.realwave.sales.manager.domain.DomainEvent
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderCreated
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderDeleted
import br.com.zup.realwave.sales.manager.infrastructure.kafka.KafkaEventContext
import br.com.zup.realwave.sales.manager.infrastructure.kafka.KafkaEventEnvelope
import br.com.zup.realwave.sales.manager.infrastructure.kafka.KafkaEventHeader
import br.com.zup.realwave.sales.manager.infrastructure.kafka.KafkaEventPayload
import br.com.zup.realwave.sales.manager.infrastructure.kafka.PurchaseOrderSnapshot
import br.com.zup.realwave.sales.manager.query.handler.PurchaseOrderEventHandlers
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PurchaseOrderEventConsumerTest {

    private lateinit var eventHandlers: PurchaseOrderEventHandlers
    private lateinit var objectMapper: ObjectMapper
    private lateinit var consumer: PurchaseOrderEventConsumer

    private val realObjectMapper = ObjectMapper().apply {
        findAndRegisterModules()
    }

    @BeforeEach
    fun setup() {
        eventHandlers = mockk()
        objectMapper = mockk()
        consumer = PurchaseOrderEventConsumer(eventHandlers, objectMapper)
    }

    @Test
    fun `should dispatch event when valid PurchaseOrderCreated message is received`() {
        val tenantId = "tenant1"
        val purchaseOrderId = "po-123"
        val eventType = "PurchaseOrderCreated"

        val envelopeJson = buildEnvelopeJson(eventType, tenantId, purchaseOrderId)
        val envelope = buildEnvelope(eventType, tenantId, purchaseOrderId)
        val payloadNode: com.fasterxml.jackson.databind.JsonNode = realObjectMapper.valueToTree(envelope.payload)
        val event = mockk<PurchaseOrderCreated>()

        every { objectMapper.readValue(envelopeJson, KafkaEventEnvelope::class.java) } returns envelope
        every { objectMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(envelope.payload) } returns payloadNode
        every { objectMapper.treeToValue(payloadNode, PurchaseOrderCreated::class.java) } returns event
        justRun { eventHandlers.dispatch(event) }

        consumer.consume(envelopeJson)

        verify(exactly = 1) { eventHandlers.dispatch(event) }
    }

    @Test
    fun `should dispatch event when valid PurchaseOrderDeleted message is received`() {
        val tenantId = "tenant2"
        val purchaseOrderId = "po-456"
        val eventType = "PurchaseOrderDeleted"

        val envelopeJson = buildEnvelopeJson(eventType, tenantId, purchaseOrderId)
        val envelope = buildEnvelope(eventType, tenantId, purchaseOrderId)
        val payloadNode: com.fasterxml.jackson.databind.JsonNode = realObjectMapper.valueToTree(envelope.payload)
        val event = mockk<PurchaseOrderDeleted>()

        every { objectMapper.readValue(envelopeJson, KafkaEventEnvelope::class.java) } returns envelope
        every { objectMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(envelope.payload) } returns payloadNode
        every { objectMapper.treeToValue(payloadNode, PurchaseOrderDeleted::class.java) } returns event
        justRun { eventHandlers.dispatch(event) }

        consumer.consume(envelopeJson)

        verify(exactly = 1) { eventHandlers.dispatch(event) }
    }

    @Test
    fun `should rethrow exception when deserialization fails`() {
        val invalidJson = """{"invalid": "json"}"""

        every { objectMapper.readValue(invalidJson, KafkaEventEnvelope::class.java) } throws RuntimeException("Deserialization error")

        assertThrows<RuntimeException> {
            consumer.consume(invalidJson)
        }
    }

    @Test
    fun `should rethrow exception when dispatch fails`() {
        val tenantId = "tenant1"
        val purchaseOrderId = "po-789"
        val eventType = "PurchaseOrderCreated"

        val envelopeJson = buildEnvelopeJson(eventType, tenantId, purchaseOrderId)
        val envelope = buildEnvelope(eventType, tenantId, purchaseOrderId)
        val payloadNode: com.fasterxml.jackson.databind.JsonNode = realObjectMapper.valueToTree(envelope.payload)
        val event = mockk<PurchaseOrderCreated>()

        every { objectMapper.readValue(envelopeJson, KafkaEventEnvelope::class.java) } returns envelope
        every { objectMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(envelope.payload) } returns payloadNode
        every { objectMapper.treeToValue(payloadNode, PurchaseOrderCreated::class.java) } returns event
        every { eventHandlers.dispatch(event) } throws RuntimeException("Handler error")

        assertThrows<RuntimeException> {
            consumer.consume(envelopeJson)
        }

        verify(exactly = 1) { eventHandlers.dispatch(event) }
    }

    private fun buildEnvelope(eventType: String, tenantId: String, purchaseOrderId: String): KafkaEventEnvelope {
        val snapshot = PurchaseOrderSnapshot(
            id = purchaseOrderId,
            status = "OPENED",
            type = null,
            customer = null,
            items = emptyList(),
            payment = br.com.zup.realwave.sales.manager.domain.Payment(mutableListOf(), null),
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
                eventType = eventType,
                timestamp = "2024-01-01T00:00:00",
                context = KafkaEventContext(tenantId = tenantId)
            ),
            payload = KafkaEventPayload(purchaseOrder = snapshot)
        )
    }

    private fun buildEnvelopeJson(eventType: String, tenantId: String, purchaseOrderId: String): String =
        """
        {
          "header": {
            "eventId": "event-id-001",
            "eventType": "$eventType",
            "timestamp": "2024-01-01T00:00:00",
            "domain": "SALES-MANAGER",
            "context": { "tenantId": "$tenantId" }
          },
          "payload": {
            "purchaseOrder": {
              "id": "$purchaseOrderId",
              "status": "OPENED"
            }
          }
        }
        """.trimIndent()
}
