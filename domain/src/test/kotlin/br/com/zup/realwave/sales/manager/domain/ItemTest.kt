package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ItemTest {

    @Test
    fun `should create Item with valid fields`() {
        val item = Item(
            catalogOfferId = "offer-1",
            price = Price.zero()
        )
        assertNotNull(item.id)
        assertEquals("offer-1", item.catalogOfferId)
        assertEquals(Price.zero(), item.price)
        assertTrue(item.offerItems.isEmpty())
        assertNull(item.validity)
        assertNull(item.pricesPerPeriod)
    }

    @Test
    fun `two Items with same id should be equal`() {
        val id = ItemId("same-id")
        val i1 = Item(id = id, catalogOfferId = "offer-1", price = Price.zero())
        val i2 = Item(id = id, catalogOfferId = "offer-1", price = Price.zero())
        assertEquals(i1, i2)
    }

    @Test
    fun `should throw exception when catalogOfferId is blank`() {
        assertThrows<IllegalArgumentException> {
            Item(catalogOfferId = "", price = Price.zero())
        }
    }
}
