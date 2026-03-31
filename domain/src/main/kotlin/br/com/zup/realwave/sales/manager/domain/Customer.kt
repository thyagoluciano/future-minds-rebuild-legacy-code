package br.com.zup.realwave.sales.manager.domain

data class Customer(val id: String) {
    init {
        require(id.isNotBlank()) { "Customer id must not be blank" }
    }
}
