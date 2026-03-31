package br.com.zup.realwave.sales.manager.domain.command

import br.com.zup.realwave.sales.manager.domain.Payment
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId

data class UpdatePaymentCommand(
    val id: PurchaseOrderId,
    val payment: Payment
)
