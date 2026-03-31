package br.com.zup.realwave.sales.manager.domain.port

import br.com.zup.realwave.sales.manager.domain.PurchaseOrder

interface PurchaseOrderValidator {
    fun validate(purchaseOrder: PurchaseOrder)
}
