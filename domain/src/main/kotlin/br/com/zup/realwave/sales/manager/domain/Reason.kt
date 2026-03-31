package br.com.zup.realwave.sales.manager.domain

data class Reason(
    val code: String,
    val message: String? = null
) {
    init {
        require(code.isNotBlank()) { "Reason code must not be blank" }
    }
}
