package br.com.zup.realwave.sales.manager.domain

import java.util.UUID

data class ItemId(val value: String = UUID.randomUUID().toString()) {
    init {
        require(value.isNotBlank()) { "ItemId value must not be blank" }
    }

    override fun toString(): String = value
}
