package br.com.zup.realwave.sales.manager.domain.event

import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderStatus
import br.com.zup.realwave.sales.manager.domain.Reason

data class PurchaseOrderStatusUpdated(
    val id: PurchaseOrderId,
    val status: PurchaseOrderStatus,
    val reason: Reason? = null
) : PurchaseOrderApplicableEvent() {

    override fun applyTo(purchaseOrder: PurchaseOrder) {
        purchaseOrder.status = status
        reason?.let { purchaseOrder.reason = it }
    }
}
