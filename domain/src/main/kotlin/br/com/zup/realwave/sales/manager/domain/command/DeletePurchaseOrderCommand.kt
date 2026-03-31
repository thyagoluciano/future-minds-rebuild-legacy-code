package br.com.zup.realwave.sales.manager.domain.command

import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId

data class DeletePurchaseOrderCommand(val id: PurchaseOrderId)
