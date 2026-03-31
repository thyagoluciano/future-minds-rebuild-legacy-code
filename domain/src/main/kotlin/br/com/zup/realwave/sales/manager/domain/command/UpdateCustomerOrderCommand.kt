package br.com.zup.realwave.sales.manager.domain.command

import br.com.zup.realwave.sales.manager.domain.CustomerOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.Reason

data class UpdateCustomerOrderCommand(
    val id: PurchaseOrderId,
    val customerOrder: CustomerOrder,
    val reason: Reason?
)
