package br.com.zup.realwave.sales.manager.api.request

import br.com.zup.realwave.sales.manager.domain.OnBoardingSale
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.command.UpdateOnBoardingSaleCommand
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.validation.constraints.NotBlank

data class OnBoardingSaleRequest(
    @field:NotBlank val id: String?,
    val customFields: JsonNode? = ObjectMapper().createObjectNode()
)

fun OnBoardingSaleRequest.toCommand(purchaseOrderId: PurchaseOrderId): UpdateOnBoardingSaleCommand {
    val mapper = jacksonObjectMapper()
    val fields: Map<String, Any> = if (customFields != null) {
        mapper.convertValue(customFields, Map::class.java) as Map<String, Any>
    } else emptyMap()

    return UpdateOnBoardingSaleCommand(
        id = purchaseOrderId,
        onBoardingSale = OnBoardingSale(customFields = fields)
    )
}
