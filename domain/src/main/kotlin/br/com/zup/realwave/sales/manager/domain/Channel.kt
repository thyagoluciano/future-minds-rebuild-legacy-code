package br.com.zup.realwave.sales.manager.domain

data class Channel(val id: String, val type: String) {
    init {
        require(id.isNotBlank()) { "Channel id must not be blank" }
        require(type.isNotBlank()) { "Channel type must not be blank" }
    }
}
