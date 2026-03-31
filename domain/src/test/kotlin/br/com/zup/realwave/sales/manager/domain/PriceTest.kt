package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PriceTest {

    @Test
    fun `should create Price with valid fields`() {
        val price = Price(currency = "BRL", amount = 1000L, scale = 2)
        assertEquals("BRL", price.currency)
        assertEquals(1000L, price.amount)
        assertEquals(2, price.scale)
    }

    @Test
    fun `should create zero Price`() {
        val price = Price.zero()
        assertEquals("BRL", price.currency)
        assertEquals(0L, price.amount)
        assertEquals(2, price.scale)
    }

    @Test
    fun `two Prices with same data should be equal`() {
        val p1 = Price("BRL", 500L, 2)
        val p2 = Price("BRL", 500L, 2)
        assertEquals(p1, p2)
    }

    @Test
    fun `should throw exception when currency is blank`() {
        assertThrows<IllegalArgumentException> { Price("", 100L, 2) }
    }

    @Test
    fun `should throw exception when amount is negative`() {
        assertThrows<IllegalArgumentException> { Price("BRL", -1L, 2) }
    }
}
