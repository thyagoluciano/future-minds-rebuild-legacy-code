package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProtocolTest {

    @Test
    fun `should create Protocol with valid value`() {
        val protocol = Protocol("PROTO-001")
        assertEquals("PROTO-001", protocol.value)
    }

    @Test
    fun `toString should return value`() {
        val protocol = Protocol("PROTO-001")
        assertEquals("PROTO-001", protocol.toString())
    }

    @Test
    fun `two Protocols with same value should be equal`() {
        val p1 = Protocol("PROTO-001")
        val p2 = Protocol("PROTO-001")
        assertEquals(p1, p2)
    }

    @Test
    fun `should throw exception when value is blank`() {
        assertThrows<IllegalArgumentException> { Protocol("") }
    }
}
