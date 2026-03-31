package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PurchaseOrderIdTest {

    @Test
    fun `should create PurchaseOrderId with given value`() {
        val id = PurchaseOrderId("abc-123")
        assertEquals("abc-123", id.value)
    }

    @Test
    fun `should create PurchaseOrderId with random UUID when no value is given`() {
        val id1 = PurchaseOrderId()
        val id2 = PurchaseOrderId()
        assertNotNull(id1.value)
        assertNotEquals(id1, id2)
    }

    @Test
    fun `toString should return value`() {
        val id = PurchaseOrderId("test-id")
        assertEquals("test-id", id.toString())
    }

    @Test
    fun `two PurchaseOrderIds with same value should be equal`() {
        val id1 = PurchaseOrderId("same-value")
        val id2 = PurchaseOrderId("same-value")
        assertEquals(id1, id2)
    }
}
