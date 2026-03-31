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
    fun `should have all defined type values`() {
        val values = PurchaseOrderType.values()
        assertTrue(values.contains(PurchaseOrderType.JOIN))
        assertTrue(values.contains(PurchaseOrderType.CHANGE))
        assertTrue(values.contains(PurchaseOrderType.BUY))
        assertTrue(values.contains(PurchaseOrderType.COUPON))
        assertTrue(values.contains(PurchaseOrderType.NORMAL))
        assertTrue(values.contains(PurchaseOrderType.ONBOARDING))
    }
}
