package br.com.zup.realwave.sales.manager.domain

data class Address(
    val street: String,
    val number: String? = null,
    val complement: String? = null,
    val neighborhood: String,
    val city: String,
    val state: String,
    val country: String,
    val zipCode: String,
    val referencePoint: String? = null
) {
    init {
        require(street.isNotBlank()) { "Address street must not be blank" }
        require(city.isNotBlank()) { "Address city must not be blank" }
        require(state.isNotBlank()) { "Address state must not be blank" }
        require(country.isNotBlank()) { "Address country must not be blank" }
        require(zipCode.isNotBlank()) { "Address zipCode must not be blank" }
    }
}
