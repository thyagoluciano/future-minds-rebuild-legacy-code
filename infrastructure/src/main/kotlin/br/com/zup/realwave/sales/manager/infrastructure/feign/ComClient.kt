package br.com.zup.realwave.sales.manager.infrastructure.feign

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(name = "com", url = "\${com.url}", configuration = [FeignConfig::class])
interface ComClient {

    @PostMapping("/customer-orders")
    fun createCustomerOrder(@RequestBody request: CustomerOrderRequest): CustomerOrderResponse

    data class CustomerOrderRequest(
        val customerId: String,
        val externalId: String,
        val externalModule: String,
        val callback: String,
        val type: String,
        val products: List<CustomerOrderProduct>,
        val offers: List<CustomerOrderOffer>,
        val payment: CustomerOrderPayment,
        val mgm: Mgm? = null,
        val subscriptionId: String? = null,
        val coupon: CouponCode? = null,
        val freight: CustomerOrderFreight? = null
    )

    data class CustomerOrderProduct(
        val productId: String? = null,
        val productTypeId: String,
        val productTypeName: String,
        val installationAttributes: Map<String, Any> = emptyMap()
    )

    data class CustomerOrderOffer(
        val catalogOfferId: String,
        val catalogOfferType: String,
        val catalogOfferName: String,
        val catalogOfferDescription: String,
        val price: PriceDto,
        val offerItems: List<CustomerOrderOfferItem>,
        val quantity: Int
    )

    data class CustomerOrderOfferItem(
        val catalogOfferItemId: String,
        val productTypeId: String,
        val productId: String?,
        val productTypeName: String?,
        val price: PriceDto,
        val recurrent: Boolean,
        val compositionId: String,
        val compositionName: String,
        val userParameters: Map<String, Any>? = null
    )

    data class CustomerOrderPayment(
        val methods: List<PaymentMethodDto>,
        val description: String? = null,
        val async: Boolean = false
    )

    data class PaymentMethodDto(
        val method: String,
        val methodId: String?,
        val price: PriceDto? = null,
        val securityCode: String? = null,
        val installments: Int? = null
    )

    data class CustomerOrderFreight(
        val address: FreightAddress,
        val price: PriceDto,
        val type: String,
        val deliveryTotalTime: Int
    )

    data class FreightAddress(
        val city: String,
        val complement: String? = null,
        val country: String,
        val district: String,
        val name: String,
        val state: String,
        val street: String,
        val zipCode: String,
        val number: String
    )

    data class Mgm(val invite: String?)

    data class CouponCode(val code: String, val discounts: List<CouponDiscount>? = emptyList())

    data class CouponDiscount(val description: String? = null, val price: PriceDto)

    data class PriceDto(
        val amount: Long,
        val currency: String,
        val scale: Int = 2
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class CustomerOrderResponse(
        val id: String,
        val externalId: String?,
        val status: String?
    )
}
