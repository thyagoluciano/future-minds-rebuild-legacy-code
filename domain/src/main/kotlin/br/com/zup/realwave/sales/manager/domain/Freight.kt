package br.com.zup.realwave.sales.manager.domain

data class Freight(
    val price: Price,
    val deliveryEstimateBusinessDays: Int? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val type: String? = null,
    val address: Address? = null
)
