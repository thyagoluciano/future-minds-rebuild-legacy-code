package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ItemIdTest {

    @Test
    fun `should create ItemId with given value`() {
        val id = ItemId("item-123")
        assertEquals("item-123", id.value)
    }

    @Test
    fun `should create ItemId with random UUID when no value given`() {
        val id1 = ItemId()
        val id2 = ItemId()
        assertNotNull(id1.value)
        assertNotEquals(id1, id2)
    }

    @Test
    fun `toString should return value`() {
        val id = ItemId("test")
        assertEquals("test", id.toString())
    }

    @Test
    fun `should throw exception when value is blank`() {
        assertThrows<IllegalArgumentException> { ItemId("") }
    }
}
