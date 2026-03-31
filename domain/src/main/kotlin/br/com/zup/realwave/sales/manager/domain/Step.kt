package br.com.zup.realwave.sales.manager.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Step(
    val step: String?,
    val status: String?,
    val startedAt: String?,
    val endedAt: String?,
    val processed: Int?,
    val total: Int?
)
