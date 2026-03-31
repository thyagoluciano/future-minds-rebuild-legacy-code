package br.com.zup.realwave.sales.manager.domain.command

import br.com.zup.realwave.sales.manager.domain.Callback
import br.com.zup.realwave.sales.manager.domain.Customer
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderType

data class CreatePurchaseOrderCommand(
    val id: PurchaseOrderId = PurchaseOrderId(),
    val purchaseOrderType: PurchaseOrderType?,
    val callback: Callback?,
    val customer: Customer? = null
)
