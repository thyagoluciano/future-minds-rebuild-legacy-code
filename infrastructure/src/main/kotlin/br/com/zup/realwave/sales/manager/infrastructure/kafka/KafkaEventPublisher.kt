package br.com.zup.realwave.sales.manager.infrastructure.kafka

import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderStatus
import br.com.zup.realwave.sales.manager.domain.port.PurchaseOrderEventPublisher
import br.com.zup.realwave.sales.manager.infrastructure.multitenant.TenantContext
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

@Component
class KafkaEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) : PurchaseOrderEventPublisher {

    companion object {
        const val TOPIC = "rw_sm_purchase_events"
    }

    override fun publish(purchaseOrder: PurchaseOrder) {
        val eventType = when (purchaseOrder.status) {
            PurchaseOrderStatus.OPENED -> "PurchaseOrderCreated"
            PurchaseOrderStatus.CHECKED_OUT -> "PurchaseOrderCheckedout"
            else -> "PurchaseOrderFinished"
        }

        val envelope = KafkaEventEnvelope(
            header = KafkaEventHeader(
                eventId = UUID.randomUUID().toString(),
                eventType = eventType,
                timestamp = LocalDateTime.now().toString(),
                context = KafkaEventContext(tenantId = TenantContext.get())
            ),
            payload = KafkaEventPayload(
                purchaseOrder = PurchaseOrderSnapshot.from(purchaseOrder)
            )
        )

        kafkaTemplate.send(TOPIC, purchaseOrder.id.value, objectMapper.writeValueAsString(envelope))
    }
}
