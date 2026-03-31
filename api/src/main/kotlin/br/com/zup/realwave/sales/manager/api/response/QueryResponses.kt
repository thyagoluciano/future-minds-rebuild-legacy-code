package br.com.zup.realwave.sales.manager.api.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class PurchaseOrderResponse(
    val id: String,
    val type: String?,
    val protocol: String?,
    var subscriptionId: String?,
    val segmentation: JsonNode?,
    val mgm: MgmResponse?,
    val salesForce: SalesForceResponse?,
    val onBoardingSale: OnBoardingSaleResponse?,
    val customer: CustomerResponse?,
    val coupon: CouponResponse?,
    val totalPrice: PriceResponse?,
    val discount: DiscountResponse?,
    val payment: PaymentResponse,
    val freight: FreightResponse? = null,
    val status: String?,
    val items: List<ItemResponse>?,
    val installationAttributes: List<InstallationAttributesResponse>?,
    val channelCreate: ChannelResponse?,
    val channelCheckout: ChannelResponse?,
    val callback: CallbackResponse?,
    val reason: ReasonResponse?,
    val createdAt: String?,
    val updatedAt: String?
)

class ChannelResponse(val value: String?)

data class CallbackResponse(val url: String, val headers: JsonNode?)

data class CustomerOrderStatusResponse(
    val customerOrderId: String?,
    val status: String?,
    val steps: List<StepResponse>?
)

data class StepResponse(
    val step: String?,
    val status: String?,
    val startedAt: String?,
    val endedAt: String?
)

data class ItemResponse(
    val id: String?,
    val catalogOfferId: String?,
    val catalogOfferType: String?,
    val price: PriceResponse,
    val validity: OfferValidityResponse,
    val customFields: JsonNode?,
    val offerItems: List<OfferItemResponse>,
    val pricesPerPeriod: List<PricePerPeriodResponse>,
    val quantity: Int? = 1
)

data class OfferItemResponse(
    val productId: String?,
    val catalogOfferItemId: String,
    val price: PriceResponse,
    val customFields: JsonNode?,
    val recurrent: Boolean?,
    val userParameters: Map<String, Any>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PriceResponse(
    @field:NotBlank val currency: String,
    @field:NotNull val amount: Int,
    @field:NotNull val scale: Int
)

data class OfferValidityResponse(
    val period: String?,
    val duration: Int?,
    val unlimited: Boolean
)

data class DiscountResponse(
    val fullPrice: PriceResponse?,
    val discountValue: PriceResponse?,
    val discountType: String?,
    val finalPrice: PriceResponse?
)

data class DescriptionResponse(val value: String?)

data class CustomerResponse(val id: String?)

data class MgmResponse(val code: String?, val fields: JsonNode?)

data class SalesForceResponse(val id: String?, val name: String?)

data class CouponResponse(val id: String?, val fields: JsonNode?)

data class OnBoardingSaleResponse(val id: String?, val fields: JsonNode?)

data class PaymentResponse(val methods: List<PaymentMethodResponse>?, val description: DescriptionResponse?)

data class PaymentMethodResponse(
    val method: String? = null,
    val methodId: String? = null,
    val price: PriceResponse? = null,
    val customFields: JsonNode?,
    val securityCodeInformed: Boolean,
    val installments: Int? = null
)

data class FreightResponse(
    val address: FreightAddressResponse,
    val price: FreightPriceResponse,
    val type: String,
    val deliveryTotalTime: Int
) {
    data class FreightAddressResponse(
        val city: String,
        val complement: String,
        val country: String,
        val district: String,
        val name: String,
        val state: String,
        val street: String,
        val number: String,
        val zipCode: String
    )

    data class FreightPriceResponse(
        val currency: String,
        val amount: Int,
        val scale: Int
    )
}

data class InstallationAttributesResponse(val productTypeId: String, val attributes: Map<String, Any>)

data class PurchaseOrderStatusResponse(val status: String?, val customerOrder: CustomerOrderStatusResponse?)

data class ReasonResponse(val code: String?, val description: String?)

data class PricePerPeriodResponse(
    val totalPrice: PriceResponse,
    val totalDiscountPrice: PriceResponse,
    val totalPriceWithDiscount: PriceResponse,
    val startAt: Int,
    val endAt: Int,
    val items: List<PricePerPeriodItemResponse>
) {

    data class PricePerPeriodItemResponse(
        val compositionId: String,
        val itemId: String,
        val price: PriceResponse,
        val discountPrice: PriceResponse,
        val priceWithDiscount: PriceResponse
    )
}
