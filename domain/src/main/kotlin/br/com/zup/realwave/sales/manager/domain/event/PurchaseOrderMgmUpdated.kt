package br.com.zup.realwave.sales.manager.domain.event

import br.com.zup.realwave.sales.manager.domain.Mgm
import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId

data class PurchaseOrderMgmUpdated(
    val id: PurchaseOrderId,
    val mgm: Mgm
) : PurchaseOrderApplicableEvent() {

    override fun applyTo(purchaseOrder: PurchaseOrder) {
        purchaseOrder.mgm = mgm
    }
}
