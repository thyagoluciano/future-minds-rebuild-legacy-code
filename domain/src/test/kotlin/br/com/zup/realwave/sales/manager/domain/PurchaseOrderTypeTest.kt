package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PurchaseOrderTypeTest {

    @Test
    fun `should have all required type values`() {
        val values = PurchaseOrderType.values()
        assertTrue(values.contains(PurchaseOrderType.NORMAL))
        assertTrue(values.contains(PurchaseOrderType.ONBOARDING))
    }

    @Test
    fun `should have exactly 2 type values`() {
        assertEquals(2, PurchaseOrderType.values().size)
    }
}
