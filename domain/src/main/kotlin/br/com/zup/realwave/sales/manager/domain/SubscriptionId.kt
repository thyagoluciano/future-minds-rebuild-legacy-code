package br.com.zup.realwave.sales.manager.domain

data class SubscriptionId(val value: String) {
    init {
        require(value.isNotBlank()) { "SubscriptionId value must not be blank" }
    }

    override fun toString(): String = value
}
