package br.com.zup.realwave.sales.manager.domain.command

import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.Segmentation

data class UpdateSegmentationCommand(
    val id: PurchaseOrderId,
    val segmentation: Segmentation
)
