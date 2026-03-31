package br.com.zup.realwave.sales.manager.api.request

import br.com.zup.realwave.sales.manager.api.request.validation.PaymentMethodValidation
import br.com.zup.realwave.sales.manager.domain.Payment
import br.com.zup.realwave.sales.manager.domain.PaymentMethod
import br.com.zup.realwave.sales.manager.domain.Price
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.command.UpdatePaymentCommand
import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class PaymentRequest(
    @field:[Valid] val methods: List<PaymentMethodRequest>,
    val description: String?,
    val async: Boolean? = false
) {
    @PaymentMethodValidation
    data class PaymentMethodRequest(
        @field:NotBlank val method: String?,
        val methodId: String?,
        val price: PaymentPrice? = null,
        val customFields: JsonNode?,
        val installments: Int? = null
    )

    data class PaymentPrice(
        @field:NotBlank val currency: String,
        @field:NotNull val amount: Int,
        @field:NotNull val scale: Int
    )
}

fun PaymentRequest.toCommand(purchaseOrderId: PurchaseOrderId): UpdatePaymentCommand = UpdatePaymentCommand(
    id = purchaseOrderId,
    payment = Payment(
        methods = methods.map { method ->
            PaymentMethod(
                type = method.method!!,
                installments = method.installments,
                totalValue = method.price?.let {
                    Price(currency = it.currency, amount = it.amount.toLong(), scale = it.scale)
                }
            )
        }.toMutableList(),
        description = description
    )
)
