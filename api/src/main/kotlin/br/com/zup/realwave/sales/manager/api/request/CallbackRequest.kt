package br.com.zup.realwave.sales.manager.api.request

import com.fasterxml.jackson.databind.JsonNode

data class CallbackRequest(val url: String, val headers: JsonNode?)
