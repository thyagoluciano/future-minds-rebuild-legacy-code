package br.com.zup.realwave.sales.manager.domain

data class Mgm(
    val code: String,
    val customFields: Map<String, Any>? = null
) {
    init {
        require(code.isNotBlank()) { "Mgm code must not be blank" }
    }
}
