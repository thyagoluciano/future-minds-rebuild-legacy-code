package br.com.zup.realwave.sales.manager.domain.command

import br.com.zup.realwave.sales.manager.domain.Item
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId

data class AddItemCommand(
    val id: PurchaseOrderId,
    val item: Item
)
