package br.com.zup.realwave.sales.manager.domain

data class Callback(
    val url: String,
    val headers: Map<String, String> = emptyMap()
) {
    init {
        require(url.isNotBlank()) { "Callback url must not be blank" }
    }
}
