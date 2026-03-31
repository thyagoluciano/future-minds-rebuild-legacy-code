package br.com.zup.realwave.sales.manager.infrastructure.kafka

import br.com.zup.realwave.sales.manager.domain.CouponCode
import br.com.zup.realwave.sales.manager.domain.Customer
import br.com.zup.realwave.sales.manager.domain.CustomerOrder
import br.com.zup.realwave.sales.manager.domain.Freight
import br.com.zup.realwave.sales.manager.domain.Item
import br.com.zup.realwave.sales.manager.domain.Mgm
import br.com.zup.realwave.sales.manager.domain.OnBoardingSale
import br.com.zup.realwave.sales.manager.domain.Payment
import br.com.zup.realwave.sales.manager.domain.Protocol
import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderStatus
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderType
import br.com.zup.realwave.sales.manager.domain.SalesForce
import br.com.zup.realwave.sales.manager.domain.Segmentation
import br.com.zup.realwave.sales.manager.domain.SubscriptionId

data class KafkaEventEnvelope(
    val header: KafkaEventHeader,
    val payload: KafkaEventPayload
)

data class KafkaEventHeader(
    val eventId: String,
    val eventType: String,
    val timestamp: String,
    val domain: String = "SALES-MANAGER",
    val context: KafkaEventContext
)

data class KafkaEventContext(
    val tenantId: String
)

data class KafkaEventPayload(
    val purchaseOrder: PurchaseOrderSnapshot
)

data class PurchaseOrderSnapshot(
    val id: String,
    val status: String,
    val type: String?,
    val customer: Customer?,
    val items: List<Item>,
    val payment: Payment,
    val freight: Freight?,
    val coupon: CouponCode?,
    val customerOrder: CustomerOrder?,
    val protocol: Protocol?,
    val subscriptionId: SubscriptionId?,
    val segmentation: Segmentation?,
    val onBoardingSale: OnBoardingSale?,
    val mgm: Mgm?,
    val salesForce: SalesForce?,
    val createdAt: String?,
    val updatedAt: String?
) {
    companion object {
        fun from(purchaseOrder: PurchaseOrder): PurchaseOrderSnapshot = PurchaseOrderSnapshot(
            id = purchaseOrder.id.value,
            status = purchaseOrder.status.name,
            type = purchaseOrder.type?.name,
            customer = purchaseOrder.customer,
            items = purchaseOrder.items.toList(),
            payment = purchaseOrder.payment,
            freight = purchaseOrder.freight,
            coupon = purchaseOrder.coupon,
            customerOrder = purchaseOrder.customerOrder,
            protocol = purchaseOrder.protocol,
            subscriptionId = purchaseOrder.subscriptionId,
            segmentation = purchaseOrder.segmentation,
            onBoardingSale = purchaseOrder.onBoardingSale,
            mgm = purchaseOrder.mgm,
            salesForce = purchaseOrder.salesForce,
            createdAt = purchaseOrder.createdAt,
            updatedAt = purchaseOrder.updatedAt
        )
    }
}
