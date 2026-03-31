package br.com.zup.realwave.sales.manager.consumer.listener

import br.com.zup.realwave.sales.manager.consumer.service.CallbackService
import br.com.zup.realwave.sales.manager.infrastructure.kafka.KafkaEventEnvelope
import br.com.zup.realwave.sales.manager.infrastructure.multitenant.TenantContext
import br.com.zup.realwave.sales.manager.query.repository.PurchaseOrderQueryRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class PurchaseOrderStatusConsumer(
    private val callbackService: CallbackService,
    private val queryRepository: PurchaseOrderQueryRepository,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(PurchaseOrderStatusConsumer::class.java)

    @KafkaListener(
        topics = ["\${kafka.topic.purchase-events:rw_sm_purchase_events}"],
        groupId = "\${kafka.consumer.group-id:sm-purchase-order-status}"
    )
    fun consume(message: String) {
        try {
            val envelope = objectMapper.readValue(message, KafkaEventEnvelope::class.java)
            TenantContext.set(envelope.header.context.tenantId)

            val purchaseOrderId = envelope.payload.purchaseOrder.id
            val purchaseOrder = queryRepository.findById(purchaseOrderId) ?: return

            val callback = purchaseOrder.callback
            if (callback != null) {
                callbackService.notify(callback, envelope)
            }
        } catch (ex: Exception) {
            logger.error("Error processing Kafka message: ${ex.message}", ex)
        } finally {
            TenantContext.clear()
        }
    }
}
