package br.com.zup.realwave.sales.manager.api.request

import br.com.zup.realwave.sales.manager.domain.CouponCode
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.command.UpdateCouponCommand
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.validation.constraints.NotBlank

data class CouponRequest(
    @field:NotBlank val code: String?,
    val customFields: JsonNode? = ObjectMapper().createObjectNode()
)

fun CouponRequest.toCommand(purchaseOrderId: PurchaseOrderId): UpdateCouponCommand = UpdateCouponCommand(
    id = purchaseOrderId,
    coupon = CouponCode(code = code!!)
)
