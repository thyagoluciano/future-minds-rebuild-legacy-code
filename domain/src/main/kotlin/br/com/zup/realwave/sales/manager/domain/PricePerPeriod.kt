package br.com.zup.realwave.sales.manager.domain

data class PricePerPeriod(
    val period: String,
    val price: Price
) {
    init {
        require(period.isNotBlank()) { "PricePerPeriod period must not be blank" }
    }
}
