package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CustomerTest {

    @Test
    fun `should create Customer with valid id`() {
        val customer = Customer("customer-123")
        assertEquals("customer-123", customer.id)
    }

    @Test
    fun `two Customers with same id should be equal`() {
        val c1 = Customer("same-id")
        val c2 = Customer("same-id")
        assertEquals(c1, c2)
    }

    @Test
    fun `should throw exception when id is blank`() {
        assertThrows<IllegalArgumentException> { Customer("") }
        assertThrows<IllegalArgumentException> { Customer("   ") }
    }
}
