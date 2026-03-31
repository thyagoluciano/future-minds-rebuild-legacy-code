package br.com.zup.realwave.sales.manager.domain

data class OfferValidity(
    val period: String?,
    val duration: Int?,
    val unlimited: Boolean
)
