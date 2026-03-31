package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AddressTest {

    @Test
    fun `should create Address with required fields`() {
        val address = Address(
            street = "Rua A",
            neighborhood = "Bairro B",
            city = "São Paulo",
            state = "SP",
            country = "BR",
            zipCode = "01310-100"
        )
        assertEquals("Rua A", address.street)
        assertEquals("São Paulo", address.city)
        assertNull(address.number)
        assertNull(address.complement)
    }

    @Test
    fun `two Addresses with same data should be equal`() {
        val a1 = Address("Rua A", null, null, "Bairro", "SP City", "SP", "BR", "01000-000")
        val a2 = Address("Rua A", null, null, "Bairro", "SP City", "SP", "BR", "01000-000")
        assertEquals(a1, a2)
    }

    @Test
    fun `should throw exception when street is blank`() {
        assertThrows<IllegalArgumentException> {
            Address("", null, null, "Bairro", "City", "SP", "BR", "01000-000")
        }
    }

    @Test
    fun `should throw exception when city is blank`() {
        assertThrows<IllegalArgumentException> {
            Address("Rua A", null, null, "Bairro", "", "SP", "BR", "01000-000")
        }
    }
}
