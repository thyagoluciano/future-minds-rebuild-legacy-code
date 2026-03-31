package br.com.zup.realwave.sales.manager.api.request

import br.com.zup.realwave.sales.manager.domain.InstallationAttribute
import br.com.zup.realwave.sales.manager.domain.ProductTypeId
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.command.UpdateInstallationAttributesCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class InstallationAttributesRequest(
    @field:NotBlank val productTypeId: String?,
    @field:NotEmpty val attributes: Map<String, Any>?
)

fun InstallationAttributesRequest.toCommand(purchaseOrderId: PurchaseOrderId): UpdateInstallationAttributesCommand =
    UpdateInstallationAttributesCommand(
        id = purchaseOrderId,
        installationAttribute = InstallationAttribute(
            productTypeId = ProductTypeId(productTypeId!!),
            attributes = attributes!!
        )
    )
