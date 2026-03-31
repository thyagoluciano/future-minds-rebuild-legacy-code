package br.com.zup.realwave.sales.manager.domain.event

import br.com.zup.realwave.sales.manager.domain.Item
import br.com.zup.realwave.sales.manager.domain.ItemId
import br.com.zup.realwave.sales.manager.domain.Price
import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PurchaseOrderItemRemovedTest {

    @Test
    fun `applyTo should remove the item with the given itemId from items set`() {
        val purchaseOrderId = PurchaseOrderId()
        val itemId = ItemId("item-to-remove")
        val item = Item(id = itemId, catalogOfferId = "offer-1", price = Price.zero())
        val otherItem = Item(id = ItemId("other-item"), catalogOfferId = "offer-2", price = Price.zero())

        val purchaseOrder = PurchaseOrder.empty(purchaseOrderId)
        purchaseOrder.items.add(item)
        purchaseOrder.items.add(otherItem)

        assertEquals(2, purchaseOrder.items.size)

        val event = PurchaseOrderItemRemoved(id = purchaseOrderId, itemId = itemId)
        event.applyTo(purchaseOrder)

        assertEquals(1, purchaseOrder.items.size)
        assertFalse(purchaseOrder.items.contains(item))
        assertTrue(purchaseOrder.items.contains(otherItem))
    }

    @Test
    fun `applyTo should not fail when removing a non-existent itemId`() {
        val purchaseOrderId = PurchaseOrderId()
        val item = Item(id = ItemId("item-001"), catalogOfferId = "offer-1", price = Price.zero())

        val purchaseOrder = PurchaseOrder.empty(purchaseOrderId)
        purchaseOrder.items.add(item)

        val event = PurchaseOrderItemRemoved(id = purchaseOrderId, itemId = ItemId("non-existent"))
        event.applyTo(purchaseOrder)

        assertEquals(1, purchaseOrder.items.size)
    }
}
