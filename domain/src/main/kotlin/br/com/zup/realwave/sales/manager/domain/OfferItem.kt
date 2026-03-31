package br.com.zup.realwave.sales.manager.domain

data class OfferItem(
    val id: String,
    val productId: String?,
    val price: Price,
    val quantity: Int? = null
) {
    init {
        require(id.isNotBlank()) { "OfferItem id must not be blank" }
    }
}
