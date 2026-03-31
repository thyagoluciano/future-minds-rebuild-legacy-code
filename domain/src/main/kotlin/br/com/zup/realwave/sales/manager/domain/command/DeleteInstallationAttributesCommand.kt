package br.com.zup.realwave.sales.manager.domain.command

import br.com.zup.realwave.sales.manager.domain.ProductTypeId
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId

data class DeleteInstallationAttributesCommand(
    val id: PurchaseOrderId,
    val productTypeId: ProductTypeId
)
