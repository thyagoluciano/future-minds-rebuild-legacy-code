package br.com.zup.realwave.sales.manager.query.consumer

import br.com.zup.realwave.sales.manager.domain.DomainEvent
import br.com.zup.realwave.sales.manager.infrastructure.eventstore.EventTypeRegistry
import br.com.zup.realwave.sales.manager.infrastructure.kafka.KafkaEventEnvelope
import br.com.zup.realwave.sales.manager.infrastructure.multitenant.TenantContext
import br.com.zup.realwave.sales.manager.query.handler.PurchaseOrderEventHandlers
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class PurchaseOrderEventConsumer(
    private val eventHandlers: PurchaseOrderEventHandlers,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(PurchaseOrderEventConsumer::class.java)

    @KafkaListener(
        topics = ["\${kafka.topic.purchase-events:rw_sm_purchase_events}"],
        groupId = "\${kafka.consumer.group-id:sm-query-handler}"
    )
    fun consume(message: String) {
        try {
            val envelope = objectMapper.readValue(message, KafkaEventEnvelope::class.java)
            TenantContext.set(envelope.header.context.tenantId)

            val eventType = EventTypeRegistry.resolve(envelope.header.eventType)
            val payloadNode: JsonNode = objectMapper.valueToTree(envelope.payload)
            val event = objectMapper.treeToValue(payloadNode, eventType.java) as DomainEvent

            eventHandlers.dispatch(event)
        } catch (ex: Exception) {
            log.error("Error processing Kafka message: ${ex.message}", ex)
            throw ex
        } finally {
            TenantContext.clear()
        }
    }
}
