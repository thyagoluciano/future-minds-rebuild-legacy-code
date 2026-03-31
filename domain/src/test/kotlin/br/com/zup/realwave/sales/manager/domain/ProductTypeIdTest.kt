package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProductTypeIdTest {

    @Test
    fun `should create ProductTypeId with valid value`() {
        val id = ProductTypeId("product-type-1")
        assertEquals("product-type-1", id.value)
    }

    @Test
    fun `toString should return value`() {
        val id = ProductTypeId("pt-1")
        assertEquals("pt-1", id.toString())
    }

    @Test
    fun `two ProductTypeIds with same value should be equal`() {
        val p1 = ProductTypeId("pt-1")
        val p2 = ProductTypeId("pt-1")
        assertEquals(p1, p2)
    }

    @Test
    fun `should throw exception when value is blank`() {
        assertThrows<IllegalArgumentException> { ProductTypeId("") }
    }
}
