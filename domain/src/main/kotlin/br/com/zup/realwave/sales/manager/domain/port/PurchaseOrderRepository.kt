package br.com.zup.realwave.sales.manager.domain.port

import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId

interface PurchaseOrderRepository {
    fun save(purchaseOrder: PurchaseOrder)
    fun findById(id: PurchaseOrderId): PurchaseOrder?
    fun findByIdOrThrow(id: PurchaseOrderId): PurchaseOrder
}
