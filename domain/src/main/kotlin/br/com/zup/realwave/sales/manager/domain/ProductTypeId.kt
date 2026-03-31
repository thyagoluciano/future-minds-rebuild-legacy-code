package br.com.zup.realwave.sales.manager.domain

data class ProductTypeId(val value: String) {
    init {
        require(value.isNotBlank()) { "ProductTypeId value must not be blank" }
    }

    override fun toString(): String = value
}
