package br.com.zup.realwave.sales.manager.api.request

import br.com.zup.realwave.sales.manager.domain.Protocol
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.command.UpdateProtocolCommand
import jakarta.validation.constraints.NotBlank

data class ProtocolRequest(@field:NotBlank val protocol: String)

fun ProtocolRequest.toCommand(purchaseOrderId: PurchaseOrderId): UpdateProtocolCommand = UpdateProtocolCommand(
    id = purchaseOrderId,
    protocol = Protocol(protocol)
)
