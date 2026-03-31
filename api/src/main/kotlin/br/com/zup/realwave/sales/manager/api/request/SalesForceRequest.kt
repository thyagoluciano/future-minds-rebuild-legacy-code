package br.com.zup.realwave.sales.manager.api.request

import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.SalesForce
import br.com.zup.realwave.sales.manager.domain.command.UpdateSalesForceCommand
import jakarta.validation.constraints.NotBlank

data class SalesForceRequest(
    @field:NotBlank val id: String,
    @field:NotBlank val name: String
)

fun SalesForceRequest.toCommand(purchaseOrderId: PurchaseOrderId): UpdateSalesForceCommand = UpdateSalesForceCommand(
    id = purchaseOrderId,
    salesForce = SalesForce(agentId = id)
)
