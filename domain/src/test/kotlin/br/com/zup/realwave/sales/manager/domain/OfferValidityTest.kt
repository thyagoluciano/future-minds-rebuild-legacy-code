package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OfferValidityTest {

    @Test
    fun `should create OfferValidity with all fields`() {
        val validity = OfferValidity(period = "MONTH", duration = 12, unlimited = false)
        assertEquals("MONTH", validity.period)
        assertEquals(12, validity.duration)
        assertFalse(validity.unlimited)
    }

    @Test
    fun `should create unlimited OfferValidity with null period and duration`() {
        val validity = OfferValidity(period = null, duration = null, unlimited = true)
        assertNull(validity.period)
        assertNull(validity.duration)
        assertTrue(validity.unlimited)
    }

    @Test
    fun `two OfferValidities with same data should be equal`() {
        val v1 = OfferValidity("MONTH", 12, false)
        val v2 = OfferValidity("MONTH", 12, false)
        assertEquals(v1, v2)
    }
}
