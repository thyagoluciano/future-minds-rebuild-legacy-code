package br.com.zup.realwave.sales.manager.domain.command

import br.com.zup.realwave.sales.manager.domain.Channel
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.SecurityCodeInformed

data class CheckoutCommand(
    val id: PurchaseOrderId,
    val channel: Channel,
    val securityCodes: List<SecurityCodeInformed> = emptyList()
)
