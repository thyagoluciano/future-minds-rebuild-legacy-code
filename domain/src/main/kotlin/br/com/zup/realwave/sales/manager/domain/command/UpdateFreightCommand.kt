package br.com.zup.realwave.sales.manager.domain.command

import br.com.zup.realwave.sales.manager.domain.Freight
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId

data class UpdateFreightCommand(
    val id: PurchaseOrderId,
    val freight: Freight
)
