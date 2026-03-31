package br.com.zup.realwave.sales.manager.domain.event

import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderStatus
import br.com.zup.realwave.sales.manager.domain.Reason

data class PurchaseOrderReasonStatusUpdated(
    val id: PurchaseOrderId,
    val reason: Reason?,
    val status: PurchaseOrderStatus? = null
) : PurchaseOrderApplicableEvent() {

    override fun applyTo(purchaseOrder: PurchaseOrder) {
        purchaseOrder.reason = reason
        status?.let { purchaseOrder.status = it }
    }
}
