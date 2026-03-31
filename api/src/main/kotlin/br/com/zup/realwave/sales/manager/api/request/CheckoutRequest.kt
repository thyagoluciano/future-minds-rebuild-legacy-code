package br.com.zup.realwave.sales.manager.api.request

import br.com.zup.realwave.sales.manager.domain.Channel
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.SecurityCodeInformed
import br.com.zup.realwave.sales.manager.domain.command.CheckoutCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CheckoutRequest(@field:[NotNull Valid] val paymentSecurityCodes: List<SecurityCodeRequest>?) {

    data class SecurityCodeRequest(
        @field:NotBlank val methodId: String?,
        @field:NotBlank val securityCode: String?
    )
}

fun CheckoutRequest?.toCommand(purchaseOrderId: PurchaseOrderId, channel: Channel): CheckoutCommand = CheckoutCommand(
    id = purchaseOrderId,
    channel = channel,
    securityCodes = this?.paymentSecurityCodes?.map { sc ->
        SecurityCodeInformed(
            catalogOfferItemId = sc.methodId!!,
            code = sc.securityCode!!
        )
    } ?: emptyList()
)
