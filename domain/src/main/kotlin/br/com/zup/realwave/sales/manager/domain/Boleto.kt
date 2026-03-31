package br.com.zup.realwave.sales.manager.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Boleto(val methodId: String)
