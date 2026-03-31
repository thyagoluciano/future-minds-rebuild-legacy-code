package br.com.zup.realwave.sales.manager.domain.command

import br.com.zup.realwave.sales.manager.domain.Item
import br.com.zup.realwave.sales.manager.domain.ItemId
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId

data class RemoveItemCommand(
    val id: PurchaseOrderId,
    val itemId: ItemId
)
