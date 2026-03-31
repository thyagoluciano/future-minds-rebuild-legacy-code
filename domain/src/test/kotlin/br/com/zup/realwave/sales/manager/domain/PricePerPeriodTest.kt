package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PricePerPeriodTest {

    @Test
    fun `should create PricePerPeriod with valid fields`() {
        val ppp = PricePerPeriod(period = "MONTH", price = Price.zero())
        assertEquals("MONTH", ppp.period)
        assertEquals(Price.zero(), ppp.price)
    }

    @Test
    fun `two PricePerPeriods with same data should be equal`() {
        val p1 = PricePerPeriod("MONTH", Price.zero())
        val p2 = PricePerPeriod("MONTH", Price.zero())
        assertEquals(p1, p2)
    }

    @Test
    fun `should throw exception when period is blank`() {
        assertThrows<IllegalArgumentException> {
            PricePerPeriod(period = "", price = Price.zero())
        }
    }
}
