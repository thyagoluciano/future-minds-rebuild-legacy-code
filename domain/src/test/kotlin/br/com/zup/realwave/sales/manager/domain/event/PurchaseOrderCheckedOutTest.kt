package br.com.zup.realwave.sales.manager.domain.event

import br.com.zup.realwave.sales.manager.domain.Channel
import br.com.zup.realwave.sales.manager.domain.CustomerOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderStatus
import br.com.zup.realwave.sales.manager.domain.SecurityCodeInformed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PurchaseOrderCheckedOutTest {

    @Test
    fun `applyTo should set status to CHECKED_OUT and configure customerOrder and channel`() {
        val purchaseOrderId = PurchaseOrderId()
        val channel = Channel(id = "ch-web", type = "WEB")
        val customerOrder = CustomerOrder(id = "co-001")

        val event = PurchaseOrderCheckedOut(
            id = purchaseOrderId,
            customerOrder = customerOrder,
            channel = channel,
            securityCodes = emptyList()
        )

        val purchaseOrder = PurchaseOrder.empty(purchaseOrderId)
        event.applyTo(purchaseOrder)

        assertEquals(PurchaseOrderStatus.CHECKED_OUT, purchaseOrder.status)
        assertEquals(customerOrder, purchaseOrder.customerOrder)
        assertEquals(channel, purchaseOrder.channelCheckout)
        assertTrue(purchaseOrder.securityCodeInformed.isEmpty())
    }

    @Test
    fun `applyTo should set securityCodes when provided`() {
        val purchaseOrderId = PurchaseOrderId()
        val channel = Channel(id = "ch-app", type = "MOBILE")
        val securityCode = SecurityCodeInformed(catalogOfferItemId = "offer-001", code = "1234")

        val event = PurchaseOrderCheckedOut(
            id = purchaseOrderId,
            customerOrder = null,
            channel = channel,
            securityCodes = listOf(securityCode)
        )

        val purchaseOrder = PurchaseOrder.empty(purchaseOrderId)
        event.applyTo(purchaseOrder)

        assertEquals(PurchaseOrderStatus.CHECKED_OUT, purchaseOrder.status)
        assertEquals(1, purchaseOrder.securityCodeInformed.size)
        assertEquals(securityCode, purchaseOrder.securityCodeInformed.first())
    }
}
