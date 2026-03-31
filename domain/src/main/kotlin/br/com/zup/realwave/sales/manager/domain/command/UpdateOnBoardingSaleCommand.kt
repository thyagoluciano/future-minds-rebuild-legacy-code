package br.com.zup.realwave.sales.manager.domain.command

import br.com.zup.realwave.sales.manager.domain.OnBoardingSale
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId

data class UpdateOnBoardingSaleCommand(
    val id: PurchaseOrderId,
    val onBoardingSale: OnBoardingSale
)
