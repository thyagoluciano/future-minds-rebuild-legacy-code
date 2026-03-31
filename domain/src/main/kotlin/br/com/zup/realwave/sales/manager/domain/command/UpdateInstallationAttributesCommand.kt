package br.com.zup.realwave.sales.manager.domain.command

import br.com.zup.realwave.sales.manager.domain.InstallationAttribute
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId

data class UpdateInstallationAttributesCommand(
    val id: PurchaseOrderId,
    val installationAttribute: InstallationAttribute
)
