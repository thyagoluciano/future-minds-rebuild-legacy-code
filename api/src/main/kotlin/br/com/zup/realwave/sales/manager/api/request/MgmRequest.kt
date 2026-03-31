package br.com.zup.realwave.sales.manager.api.request

import br.com.zup.realwave.sales.manager.domain.Mgm
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.command.UpdateMgmCommand
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.validation.constraints.NotBlank

data class MgmRequest(
    @field:NotBlank val code: String?,
    val customFields: JsonNode? = ObjectMapper().createObjectNode()
)

fun MgmRequest.toCommand(purchaseOrderId: PurchaseOrderId): UpdateMgmCommand {
    val mapper = jacksonObjectMapper()
    val fields: Map<String, Any>? = if (customFields != null) {
        mapper.convertValue(customFields, Map::class.java) as Map<String, Any>
    } else null

    return UpdateMgmCommand(
        id = purchaseOrderId,
        mgm = Mgm(code = code!!, customFields = fields)
    )
}
