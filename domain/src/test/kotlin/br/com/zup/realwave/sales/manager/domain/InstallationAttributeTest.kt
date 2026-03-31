package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class InstallationAttributeTest {

    @Test
    fun `should create InstallationAttribute with valid fields`() {
        val productTypeId = ProductTypeId("pt-1")
        val attributes = mapOf<String, Any>("key" to "value")
        val ia = InstallationAttribute(productTypeId = productTypeId, attributes = attributes)
        assertEquals(productTypeId, ia.productTypeId)
        assertEquals(attributes, ia.attributes)
    }

    @Test
    fun `two InstallationAttributes with same data should be equal`() {
        val ia1 = InstallationAttribute(ProductTypeId("pt-1"), mapOf("k" to "v"))
        val ia2 = InstallationAttribute(ProductTypeId("pt-1"), mapOf("k" to "v"))
        assertEquals(ia1, ia2)
    }
}
