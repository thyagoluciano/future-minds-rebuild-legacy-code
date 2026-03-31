package br.com.zup.realwave.sales.manager.domain.command

import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.SubscriptionId

data class UpdateSubscriptionCommand(
    val id: PurchaseOrderId,
    val subscriptionId: SubscriptionId
)
