package br.com.zup.realwave.sales.manager.domain

import java.math.BigDecimal

data class Discount(
    val percentage: BigDecimal,
    val valueDiscount: Price,
    val coupon: String? = null
) {
    init {
        require(percentage >= BigDecimal.ZERO) { "Discount percentage must not be negative" }
    }
}
