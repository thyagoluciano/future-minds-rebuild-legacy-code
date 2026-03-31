package br.com.zup.realwave.sales.manager.domain.command

import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderType

data class UpdatePurchaseOrderTypeCommand(
    val id: PurchaseOrderId,
    val type: PurchaseOrderType?
)
