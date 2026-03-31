package br.com.zup.realwave.sales.manager.domain

data class Item(
    val id: ItemId = ItemId(),
    val catalogOfferId: String,
    val price: Price,
    val validity: OfferValidity? = null,
    val offerItems: List<OfferItem> = emptyList(),
    val pricesPerPeriod: List<PricePerPeriod>? = null
) {
    init {
        require(catalogOfferId.isNotBlank()) { "Item catalogOfferId must not be blank" }
    }
}
