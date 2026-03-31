package br.com.zup.realwave.sales.manager.domain.command

import br.com.zup.realwave.sales.manager.domain.Protocol
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId

data class UpdateProtocolCommand(
    val id: PurchaseOrderId,
    val protocol: Protocol
)
