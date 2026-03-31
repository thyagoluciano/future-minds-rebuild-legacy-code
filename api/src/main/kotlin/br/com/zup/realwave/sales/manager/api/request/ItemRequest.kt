package br.com.zup.realwave.sales.manager.api.request

import br.com.zup.realwave.sales.manager.api.request.validation.ItemRequestValidation
import br.com.zup.realwave.sales.manager.domain.Item
import br.com.zup.realwave.sales.manager.domain.ItemId
import br.com.zup.realwave.sales.manager.domain.OfferItem
import br.com.zup.realwave.sales.manager.domain.OfferValidity
import br.com.zup.realwave.sales.manager.domain.Price
import br.com.zup.realwave.sales.manager.domain.PricePerPeriod
import br.com.zup.realwave.sales.manager.domain.command.AddItemCommand
import br.com.zup.realwave.sales.manager.domain.command.UpdateItemCommand
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@ItemRequestValidation
data class ItemRequest(
    @field:[NotNull Valid] val catalogOfferId: String?,
    @field:[NotNull Valid] val catalogOfferType: String?,
    @field:[NotNull Valid] val price: PriceRequest?,
    @field:[NotNull Valid] val validity: OfferValidityRequest?,
    val offerFields: JsonNode? = ObjectMapper().createObjectNode(),
    val customFields: JsonNode? = ObjectMapper().createObjectNode(),
    @field:[NotNull Valid] val offerItems: List<OfferItemRequest>?,
    @field:[NotNull Valid] val pricesPerPeriod: List<PricePerPeriodRequest>? = listOf(),
    val quantity: Int = 1
) {

    data class OfferItemRequest(
        val productId: String?,
        @field:NotBlank val catalogOfferItemId: String?,
        @field:[NotNull Valid] val price: PriceRequest?,
        val recurrent: Boolean?,
        val customFields: JsonNode? = ObjectMapper().createObjectNode(),
        val userParameters: Map<String, Any>? = null
    )

    data class PricePerPeriodRequest(
        @field:[NotNull Valid] val totalPrice: PriceRequest?,
        @field:[NotNull Valid] val totalDiscountPrice: PriceRequest?,
        @field:[NotNull Valid] val totalPriceWithDiscount: PriceRequest?,
        @field:[NotNull Valid] val startAt: Int?,
        @field:[NotNull Valid] val endAt: Int?,
        @field:[NotNull Valid] val items: List<PricePerPeriodItemRequest>?
    ) {

        data class PricePerPeriodItemRequest(
            @field:[NotNull Valid] val compositionId: String?,
            @field:[NotNull Valid] val itemId: String?,
            @field:[NotNull Valid] val price: PriceRequest?,
            @field:[NotNull Valid] val discountPrice: PriceRequest?,
            @field:[NotNull Valid] val priceWithDiscount: PriceRequest?
        )
    }
}

data class OfferValidityRequest(
    val period: String?,
    val duration: Int?,
    val unlimited: Boolean
)

data class PriceRequest(
    @field:NotBlank val currency: String?,
    @field:NotNull val amount: Int?,
    @field:NotNull val scale: Int?
)

fun PriceRequest.toDomain(): Price = Price(
    currency = currency!!,
    amount = amount!!.toLong(),
    scale = scale!!
)

fun OfferValidityRequest.toDomain(): OfferValidity = OfferValidity(
    period = period,
    duration = duration,
    unlimited = unlimited
)

fun ItemRequest.OfferItemRequest.toDomain(): OfferItem = OfferItem(
    id = catalogOfferItemId!!,
    productId = productId,
    price = price!!.toDomain()
)

fun ItemRequest.toItem(itemId: ItemId = ItemId()): Item = Item(
    id = itemId,
    catalogOfferId = catalogOfferId!!,
    price = price!!.toDomain(),
    validity = validity?.toDomain(),
    offerItems = offerItems?.map { it.toDomain() } ?: emptyList()
)

fun ItemRequest.toAddCommand(purchaseOrderId: br.com.zup.realwave.sales.manager.domain.PurchaseOrderId): AddItemCommand =
    AddItemCommand(id = purchaseOrderId, item = toItem())

fun ItemRequest.toUpdateCommand(
    purchaseOrderId: br.com.zup.realwave.sales.manager.domain.PurchaseOrderId,
    itemId: ItemId
): UpdateItemCommand = UpdateItemCommand(id = purchaseOrderId, item = toItem(itemId))
