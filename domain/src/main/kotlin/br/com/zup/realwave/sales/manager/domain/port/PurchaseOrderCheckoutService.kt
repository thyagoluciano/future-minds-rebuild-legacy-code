package br.com.zup.realwave.sales.manager.domain.port

import br.com.zup.realwave.sales.manager.domain.CustomerOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.command.CheckoutCommand

interface PurchaseOrderCheckoutService {
    fun checkout(purchaseOrder: PurchaseOrder, command: CheckoutCommand): CustomerOrder
}
