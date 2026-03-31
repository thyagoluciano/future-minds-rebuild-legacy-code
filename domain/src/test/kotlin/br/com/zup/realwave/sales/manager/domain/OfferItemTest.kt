package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OfferItemTest {

    @Test
    fun `should create OfferItem with valid fields`() {
        val item = OfferItem(id = "oi-1", productId = "prod-1", price = Price.zero())
        assertEquals("oi-1", item.id)
        assertEquals("prod-1", item.productId)
        assertEquals(Price.zero(), item.price)
        assertNull(item.quantity)
    }

    @Test
    fun `should create OfferItem with null productId`() {
        val item = OfferItem(id = "oi-1", productId = null, price = Price.zero())
        assertNull(item.productId)
    }

    @Test
    fun `two OfferItems with same data should be equal`() {
        val i1 = OfferItem("oi-1", "prod-1", Price.zero())
        val i2 = OfferItem("oi-1", "prod-1", Price.zero())
        assertEquals(i1, i2)
    }

    @Test
    fun `should throw exception when id is blank`() {
        assertThrows<IllegalArgumentException> {
            OfferItem(id = "", productId = "prod-1", price = Price.zero())
        }
    }
}
