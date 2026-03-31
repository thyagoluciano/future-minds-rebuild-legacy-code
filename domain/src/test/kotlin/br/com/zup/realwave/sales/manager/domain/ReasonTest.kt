package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ReasonTest {

    @Test
    fun `should create Reason with code and message`() {
        val reason = Reason(code = "ERR_001", message = "Some error occurred")
        assertEquals("ERR_001", reason.code)
        assertEquals("Some error occurred", reason.message)
    }

    @Test
    fun `should create Reason with null message`() {
        val reason = Reason(code = "ERR_001")
        assertEquals("ERR_001", reason.code)
        assertNull(reason.message)
    }

    @Test
    fun `two Reasons with same data should be equal`() {
        val r1 = Reason("CODE")
        val r2 = Reason("CODE")
        assertEquals(r1, r2)
    }

    @Test
    fun `should throw exception when code is blank`() {
        assertThrows<IllegalArgumentException> { Reason("") }
    }
}
