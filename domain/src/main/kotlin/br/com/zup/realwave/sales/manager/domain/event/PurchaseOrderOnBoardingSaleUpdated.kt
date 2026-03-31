package br.com.zup.realwave.sales.manager.domain.event

import br.com.zup.realwave.sales.manager.domain.OnBoardingSale
import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId

data class PurchaseOrderOnBoardingSaleUpdated(
    val id: PurchaseOrderId,
    val onBoardingSale: OnBoardingSale
) : PurchaseOrderApplicableEvent() {

    override fun applyTo(purchaseOrder: PurchaseOrder) {
        purchaseOrder.onBoardingSale = onBoardingSale
    }
}
