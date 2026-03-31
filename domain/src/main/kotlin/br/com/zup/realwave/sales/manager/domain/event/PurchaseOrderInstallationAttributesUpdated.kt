package br.com.zup.realwave.sales.manager.domain.event

import br.com.zup.realwave.sales.manager.domain.InstallationAttribute
import br.com.zup.realwave.sales.manager.domain.ProductTypeId
import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId

data class PurchaseOrderInstallationAttributesUpdated(
    val id: PurchaseOrderId,
    val productTypeId: ProductTypeId,
    val installationAttribute: InstallationAttribute
) : PurchaseOrderApplicableEvent() {

    override fun applyTo(purchaseOrder: PurchaseOrder) {
        purchaseOrder.installationAttributes[productTypeId] = installationAttribute
    }
}
