package br.com.zup.realwave.sales.manager.domain

data class PaymentMethod(
    val type: String,
    val installments: Int? = null,
    val installmentValue: Price? = null,
    val totalValue: Price? = null,
    val cardToken: String? = null
) {
    init {
        require(type.isNotBlank()) { "PaymentMethod type must not be blank" }
    }
}
