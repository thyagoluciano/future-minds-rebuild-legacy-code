package br.com.zup.realwave.sales.manager.domain.event

import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.SubscriptionId

data class PurchaseOrderSubscriptionUpdated(
    val id: PurchaseOrderId,
    val subscriptionId: SubscriptionId
) : PurchaseOrderApplicableEvent() {

    override fun applyTo(purchaseOrder: PurchaseOrder) {
        purchaseOrder.subscriptionId = subscriptionId
    }
}
