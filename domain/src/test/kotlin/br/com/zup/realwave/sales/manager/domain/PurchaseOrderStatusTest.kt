package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PurchaseOrderStatusTest {

    @Test
    fun `should have all required status values`() {
        val values = PurchaseOrderStatus.values()
        assertTrue(values.contains(PurchaseOrderStatus.OPENED))
        assertTrue(values.contains(PurchaseOrderStatus.CHECKED_OUT))
        assertTrue(values.contains(PurchaseOrderStatus.COMPLETED))
        assertTrue(values.contains(PurchaseOrderStatus.FAILED))
        assertTrue(values.contains(PurchaseOrderStatus.CANCELED))
        assertTrue(values.contains(PurchaseOrderStatus.DELETED))
    }

    @Test
    fun `should have exactly 6 status values`() {
        assertEquals(6, PurchaseOrderStatus.values().size)
    }
}
