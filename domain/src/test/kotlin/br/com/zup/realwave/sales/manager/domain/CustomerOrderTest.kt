package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CustomerOrderTest {

    @Test
    fun `should create CustomerOrder with valid id`() {
        val order = CustomerOrder("order-123")
        assertEquals("order-123", order.id)
    }

    @Test
    fun `two CustomerOrders with same id should be equal`() {
        val o1 = CustomerOrder("order-1")
        val o2 = CustomerOrder("order-1")
        assertEquals(o1, o2)
    }

    @Test
    fun `should throw exception when id is blank`() {
        assertThrows<IllegalArgumentException> { CustomerOrder("") }
    }
}
