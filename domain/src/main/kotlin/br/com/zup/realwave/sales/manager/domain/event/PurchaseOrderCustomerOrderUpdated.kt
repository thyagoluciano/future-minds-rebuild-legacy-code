package br.com.zup.realwave.sales.manager.domain.event

import br.com.zup.realwave.sales.manager.domain.Channel
import br.com.zup.realwave.sales.manager.domain.CustomerOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderStatus

data class PurchaseOrderCustomerOrderUpdated(
    val id: PurchaseOrderId,
    val customerOrder: CustomerOrder,
    val status: PurchaseOrderStatus? = null,
    val channel: Channel? = null
) : PurchaseOrderApplicableEvent() {

    override fun applyTo(purchaseOrder: PurchaseOrder) {
        purchaseOrder.customerOrder = customerOrder
        status?.let { purchaseOrder.status = it }
        channel?.let { purchaseOrder.channelCheckout = it }
    }
}
