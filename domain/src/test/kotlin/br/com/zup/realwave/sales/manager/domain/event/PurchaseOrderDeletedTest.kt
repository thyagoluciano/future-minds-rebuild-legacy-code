package br.com.zup.realwave.sales.manager.domain.event

import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PurchaseOrderDeletedTest {

    @Test
    fun `applyTo should set status to DELETED`() {
        val purchaseOrderId = PurchaseOrderId()

        val event = PurchaseOrderDeleted(id = purchaseOrderId)

        val purchaseOrder = PurchaseOrder.empty(purchaseOrderId)
        // Initially OPENED
        assertEquals(PurchaseOrderStatus.OPENED, purchaseOrder.status)

        event.applyTo(purchaseOrder)

        assertEquals(PurchaseOrderStatus.DELETED, purchaseOrder.status)
    }

    @Test
    fun `applyTo should not alter other fields of purchase order`() {
        val purchaseOrderId = PurchaseOrderId()
        val event = PurchaseOrderDeleted(id = purchaseOrderId)

        val purchaseOrder = PurchaseOrder.empty(purchaseOrderId)
        event.applyTo(purchaseOrder)

        assertEquals(PurchaseOrderStatus.DELETED, purchaseOrder.status)
        assertEquals(purchaseOrderId, purchaseOrder.id)
        assertEquals(0, purchaseOrder.items.size)
    }
}
