package br.com.zup.realwave.sales.manager.domain.port

import br.com.zup.realwave.sales.manager.domain.PurchaseOrder

interface PurchaseOrderEventPublisher {
    fun publish(purchaseOrder: PurchaseOrder)
}
