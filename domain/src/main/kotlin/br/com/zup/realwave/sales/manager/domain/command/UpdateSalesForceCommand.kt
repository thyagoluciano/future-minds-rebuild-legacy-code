package br.com.zup.realwave.sales.manager.domain.command

import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.SalesForce

data class UpdateSalesForceCommand(
    val id: PurchaseOrderId,
    val salesForce: SalesForce
)
