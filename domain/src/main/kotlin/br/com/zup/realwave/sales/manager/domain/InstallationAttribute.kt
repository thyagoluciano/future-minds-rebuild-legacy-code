package br.com.zup.realwave.sales.manager.domain

data class InstallationAttribute(
    val productTypeId: ProductTypeId,
    val attributes: Map<String, Any>
)
