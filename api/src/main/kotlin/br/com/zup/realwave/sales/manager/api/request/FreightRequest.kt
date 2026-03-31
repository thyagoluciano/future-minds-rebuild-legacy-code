package br.com.zup.realwave.sales.manager.api.request

import br.com.zup.realwave.sales.manager.domain.Address
import br.com.zup.realwave.sales.manager.domain.Freight
import br.com.zup.realwave.sales.manager.domain.Price
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.command.UpdateFreightCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class FreightRequest(
    @field:Valid val address: AddressRequest,
    @field:Valid val price: FreightPriceRequest,
    @field:[Valid NotBlank] val type: String?,
    @field:[Valid NotNull] val deliveryTotalTime: Int?
) {
    data class AddressRequest(
        @field:[Valid NotBlank] val city: String?,
        val complement: String? = null,
        @field:[Valid NotBlank] val country: String?,
        @field:[Valid NotBlank] val district: String?,
        @field:[Valid NotBlank] val name: String?,
        @field:[Valid NotBlank] val state: String?,
        @field:[Valid NotBlank] val street: String?,
        @field:[Valid NotBlank] val number: String?,
        @field:[Valid NotBlank] val zipCode: String?
    )

    data class FreightPriceRequest(
        @field:[Valid NotBlank] val currency: String?,
        @field:[Valid NotNull] val amount: Int?,
        @field:[Valid NotNull] val scale: Int?
    )
}

fun FreightRequest.toCommand(purchaseOrderId: PurchaseOrderId): UpdateFreightCommand = UpdateFreightCommand(
    id = purchaseOrderId,
    freight = Freight(
        type = type,
        price = Price(
            currency = price.currency!!,
            amount = price.amount!!.toLong(),
            scale = price.scale!!
        ),
        address = Address(
            street = address.street!!,
            number = address.number,
            complement = address.complement,
            neighborhood = address.district!!,
            city = address.city!!,
            state = address.state!!,
            country = address.country!!,
            zipCode = address.zipCode!!
        ),
        deliveryEstimateBusinessDays = deliveryTotalTime
    )
)
