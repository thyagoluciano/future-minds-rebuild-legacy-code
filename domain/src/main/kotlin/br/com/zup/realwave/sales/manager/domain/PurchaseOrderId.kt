package br.com.zup.realwave.sales.manager.domain

import java.util.UUID

data class PurchaseOrderId(val value: String = UUID.randomUUID().toString()) {
    override fun toString(): String = value
}
