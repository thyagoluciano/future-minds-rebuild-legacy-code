package br.com.zup.realwave.sales.manager.domain.event

import br.com.zup.realwave.sales.manager.domain.Protocol
import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId

data class PurchaseOrderProtocolUpdated(
    val id: PurchaseOrderId,
    val protocol: Protocol
) : PurchaseOrderApplicableEvent() {

    override fun applyTo(purchaseOrder: PurchaseOrder) {
        purchaseOrder.protocol = protocol
    }
}
