package br.com.zup.realwave.sales.manager.domain.command

import br.com.zup.realwave.sales.manager.domain.Customer
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId

data class UpdateCustomerCommand(
    val id: PurchaseOrderId,
    val customer: Customer
)
