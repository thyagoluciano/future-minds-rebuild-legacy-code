package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SecurityCodeInformedTest {

    @Test
    fun `should create SecurityCodeInformed with valid fields`() {
        val sci = SecurityCodeInformed(catalogOfferItemId = "item-1", code = "123")
        assertEquals("item-1", sci.catalogOfferItemId)
        assertEquals("123", sci.code)
    }

    @Test
    fun `two SecurityCodeInformeds with same data should be equal`() {
        val s1 = SecurityCodeInformed("item-1", "123")
        val s2 = SecurityCodeInformed("item-1", "123")
        assertEquals(s1, s2)
    }

    @Test
    fun `should throw exception when catalogOfferItemId is blank`() {
        assertThrows<IllegalArgumentException> {
            SecurityCodeInformed(catalogOfferItemId = "", code = "123")
        }
    }

    @Test
    fun `should throw exception when code is blank`() {
        assertThrows<IllegalArgumentException> {
            SecurityCodeInformed(catalogOfferItemId = "item-1", code = "")
        }
    }
}
