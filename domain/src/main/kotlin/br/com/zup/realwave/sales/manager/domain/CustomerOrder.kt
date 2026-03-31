package br.com.zup.realwave.sales.manager.domain

data class CustomerOrder(val id: String) {
    init {
        require(id.isNotBlank()) { "CustomerOrder id must not be blank" }
    }
}
