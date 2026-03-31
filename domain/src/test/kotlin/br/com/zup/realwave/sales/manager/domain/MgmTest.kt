package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MgmTest {

    @Test
    fun `should create Mgm with valid code`() {
        val mgm = Mgm(code = "MGM-CODE")
        assertEquals("MGM-CODE", mgm.code)
        assertNull(mgm.customFields)
    }

    @Test
    fun `should create Mgm with custom fields`() {
        val mgm = Mgm(code = "MGM-CODE", customFields = mapOf("k" to "v"))
        assertNotNull(mgm.customFields)
    }

    @Test
    fun `two Mgms with same data should be equal`() {
        val m1 = Mgm("CODE")
        val m2 = Mgm("CODE")
        assertEquals(m1, m2)
    }

    @Test
    fun `should throw exception when code is blank`() {
        assertThrows<IllegalArgumentException> { Mgm("") }
    }
}
