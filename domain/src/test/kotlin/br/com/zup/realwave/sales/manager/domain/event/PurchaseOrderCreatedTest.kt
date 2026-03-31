package br.com.zup.realwave.sales.manager.domain.event

import br.com.zup.realwave.sales.manager.domain.Callback
import br.com.zup.realwave.sales.manager.domain.Channel
import br.com.zup.realwave.sales.manager.domain.Customer
import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderStatus
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PurchaseOrderCreatedTest {

    @Test
    fun `applyTo should set status to OPENED and configure customer and channel`() {
        val id = PurchaseOrderId()
        val customer = Customer("customer-001")
        val channel = Channel(id = "ch-1", type = "WEB")
        val callback = Callback(url = "https://example.com/callback")

        val event = PurchaseOrderCreated(
            id = id,
            type = PurchaseOrderType.NORMAL,
            customer = customer,
            callback = callback,
            channel = channel
        )

        val purchaseOrder = PurchaseOrder.empty(id)
        event.applyTo(purchaseOrder)

        assertEquals(PurchaseOrderStatus.OPENED, purchaseOrder.status)
        assertEquals(customer, purchaseOrder.customer)
        assertEquals(channel, purchaseOrder.channelCreate)
        assertEquals(callback, purchaseOrder.callback)
        assertEquals(PurchaseOrderType.NORMAL, purchaseOrder.type)
    }

    @Test
    fun `applyTo should allow null customer and channel`() {
        val id = PurchaseOrderId()

        val event = PurchaseOrderCreated(
            id = id,
            type = null,
            customer = null,
            callback = null,
            channel = null
        )

        val purchaseOrder = PurchaseOrder.empty(id)
        event.applyTo(purchaseOrder)

        assertEquals(PurchaseOrderStatus.OPENED, purchaseOrder.status)
        assertNull(purchaseOrder.customer)
        assertNull(purchaseOrder.channelCreate)
        assertNull(purchaseOrder.callback)
        assertNull(purchaseOrder.type)
    }
}
