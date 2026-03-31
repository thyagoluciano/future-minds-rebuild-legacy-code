package br.com.zup.realwave.sales.manager.domain

import java.math.BigDecimal

data class Price(
    val currency: String,
    val amount: Long,
    val scale: Int = 2
) {
    init {
        require(currency.isNotBlank()) { "Price currency must not be blank" }
        require(amount >= 0) { "Price amount must not be negative" }
        require(scale >= 0) { "Price scale must not be negative" }
    }

    companion object {
        private const val DEFAULT_CURRENCY = "BRL"

        fun zero(): Price = Price(
            currency = DEFAULT_CURRENCY,
            amount = 0L,
            scale = 2
        )
    }

    override fun toString(): String {
        return "${BigDecimal.valueOf(amount, scale)} $currency"
    }
}
