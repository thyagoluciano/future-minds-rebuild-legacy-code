package br.com.zup.realwave.sales.manager.domain.event

import br.com.zup.realwave.sales.manager.domain.Callback
import br.com.zup.realwave.sales.manager.domain.Channel
import br.com.zup.realwave.sales.manager.domain.Customer
import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderStatus
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderType

data class PurchaseOrderCreated(
    val id: PurchaseOrderId,
    val type: PurchaseOrderType?,
    val customer: Customer?,
    val callback: Callback?,
    val channel: Channel?
) : PurchaseOrderApplicableEvent() {

    override fun applyTo(purchaseOrder: PurchaseOrder) {
        purchaseOrder.id = id
        purchaseOrder.status = PurchaseOrderStatus.OPENED
        purchaseOrder.type = type
        purchaseOrder.customer = customer
        purchaseOrder.callback = callback
        purchaseOrder.channelCreate = channel
    }
}
