package br.com.zup.realwave.sales.manager.domain.event

import br.com.zup.realwave.sales.manager.domain.Channel
import br.com.zup.realwave.sales.manager.domain.CustomerOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderStatus
import br.com.zup.realwave.sales.manager.domain.SecurityCodeInformed

data class PurchaseOrderCheckedOut(
    val id: PurchaseOrderId,
    val customerOrder: CustomerOrder? = null,
    val channel: Channel,
    val securityCodes: List<SecurityCodeInformed> = emptyList()
) : PurchaseOrderApplicableEvent() {

    override fun applyTo(purchaseOrder: PurchaseOrder) {
        purchaseOrder.status = PurchaseOrderStatus.CHECKED_OUT
        purchaseOrder.customerOrder = customerOrder
        purchaseOrder.channelCheckout = channel
        purchaseOrder.securityCodeInformed = securityCodes
    }
}
