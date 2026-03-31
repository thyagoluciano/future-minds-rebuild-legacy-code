package br.com.zup.realwave.sales.manager.domain.event

import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId

data class PurchaseOrderSalesForceRemoved(
    val id: PurchaseOrderId
) : PurchaseOrderApplicableEvent() {

    override fun applyTo(purchaseOrder: PurchaseOrder) {
        purchaseOrder.salesForce = null
    }
}
