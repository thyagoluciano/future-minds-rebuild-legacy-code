package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ChannelTest {

    @Test
    fun `should create Channel with valid fields`() {
        val channel = Channel(id = "ch-1", type = "WEB")
        assertEquals("ch-1", channel.id)
        assertEquals("WEB", channel.type)
    }

    @Test
    fun `two Channels with same fields should be equal`() {
        val c1 = Channel("ch-1", "WEB")
        val c2 = Channel("ch-1", "WEB")
        assertEquals(c1, c2)
    }

    @Test
    fun `should throw exception when id is blank`() {
        assertThrows<IllegalArgumentException> { Channel("", "WEB") }
    }

    @Test
    fun `should throw exception when type is blank`() {
        assertThrows<IllegalArgumentException> { Channel("ch-1", "") }
    }
}
