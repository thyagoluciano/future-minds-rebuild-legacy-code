package br.com.zup.realwave.sales.manager.domain.command

import br.com.zup.realwave.sales.manager.domain.Mgm
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId

data class UpdateMgmCommand(
    val id: PurchaseOrderId,
    val mgm: Mgm
)
