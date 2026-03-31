package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SubscriptionIdTest {

    @Test
    fun `should create SubscriptionId with valid value`() {
        val id = SubscriptionId("sub-123")
        assertEquals("sub-123", id.value)
    }

    @Test
    fun `toString should return value`() {
        val id = SubscriptionId("sub-123")
        assertEquals("sub-123", id.toString())
    }

    @Test
    fun `two SubscriptionIds with same value should be equal`() {
        val s1 = SubscriptionId("sub-1")
        val s2 = SubscriptionId("sub-1")
        assertEquals(s1, s2)
    }

    @Test
    fun `should throw exception when value is blank`() {
        assertThrows<IllegalArgumentException> { SubscriptionId("") }
    }
}
