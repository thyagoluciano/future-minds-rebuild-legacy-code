package br.com.zup.realwave.sales.manager.api.request

import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.SubscriptionId
import br.com.zup.realwave.sales.manager.domain.command.UpdateSubscriptionCommand
import jakarta.validation.constraints.NotBlank

data class SubscriptionRequest(@field:NotBlank val id: String)

fun SubscriptionRequest.toCommand(purchaseOrderId: PurchaseOrderId): UpdateSubscriptionCommand =
    UpdateSubscriptionCommand(
        id = purchaseOrderId,
        subscriptionId = SubscriptionId(id)
    )
