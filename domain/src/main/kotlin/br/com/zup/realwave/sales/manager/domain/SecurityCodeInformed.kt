package br.com.zup.realwave.sales.manager.domain

data class SecurityCodeInformed(
    val catalogOfferItemId: String,
    val code: String
) {
    init {
        require(catalogOfferItemId.isNotBlank()) { "SecurityCodeInformed catalogOfferItemId must not be blank" }
        require(code.isNotBlank()) { "SecurityCodeInformed code must not be blank" }
    }
}
