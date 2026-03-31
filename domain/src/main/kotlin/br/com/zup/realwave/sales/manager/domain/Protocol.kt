package br.com.zup.realwave.sales.manager.domain

data class Protocol(val value: String) {
    init {
        require(value.isNotBlank()) { "Protocol value must not be blank" }
    }

    override fun toString(): String = value
}
