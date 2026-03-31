package br.com.zup.realwave.sales.manager.domain.event

import br.com.zup.realwave.sales.manager.domain.ProductTypeId
import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId

data class PurchaseOrderInstallationAttributesDeleted(
    val id: PurchaseOrderId,
    val productTypeId: ProductTypeId
) : PurchaseOrderApplicableEvent() {

    override fun applyTo(purchaseOrder: PurchaseOrder) {
        purchaseOrder.installationAttributes.remove(productTypeId)
    }
}
