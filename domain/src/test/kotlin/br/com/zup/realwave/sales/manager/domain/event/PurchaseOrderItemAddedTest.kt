package br.com.zup.realwave.sales.manager.domain.event

import br.com.zup.realwave.sales.manager.domain.Item
import br.com.zup.realwave.sales.manager.domain.ItemId
import br.com.zup.realwave.sales.manager.domain.Price
import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PurchaseOrderItemAddedTest {

    @Test
    fun `applyTo should add item to the purchase order items set`() {
        val purchaseOrderId = PurchaseOrderId()
        val item = Item(
            id = ItemId("item-001"),
            catalogOfferId = "offer-abc",
            price = Price.zero()
        )

        val event = PurchaseOrderItemAdded(id = purchaseOrderId, item = item)

        val purchaseOrder = PurchaseOrder.empty(purchaseOrderId)
        assertTrue(purchaseOrder.items.isEmpty())

        event.applyTo(purchaseOrder)

        assertEquals(1, purchaseOrder.items.size)
        assertTrue(purchaseOrder.items.contains(item))
    }

    @Test
    fun `applyTo should add multiple distinct items`() {
        val purchaseOrderId = PurchaseOrderId()
        val item1 = Item(id = ItemId("item-001"), catalogOfferId = "offer-1", price = Price.zero())
        val item2 = Item(id = ItemId("item-002"), catalogOfferId = "offer-2", price = Price.zero())

        val purchaseOrder = PurchaseOrder.empty(purchaseOrderId)

        PurchaseOrderItemAdded(id = purchaseOrderId, item = item1).applyTo(purchaseOrder)
        PurchaseOrderItemAdded(id = purchaseOrderId, item = item2).applyTo(purchaseOrder)

        assertEquals(2, purchaseOrder.items.size)
        assertTrue(purchaseOrder.items.contains(item1))
        assertTrue(purchaseOrder.items.contains(item2))
    }
}
