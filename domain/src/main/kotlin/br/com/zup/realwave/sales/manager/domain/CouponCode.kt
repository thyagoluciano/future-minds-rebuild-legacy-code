package br.com.zup.realwave.sales.manager.domain

data class CouponCode(val code: String) {
    init {
        require(code.isNotBlank()) { "CouponCode code must not be blank" }
    }
}
