package br.com.zup.realwave.sales.manager.domain.command

import br.com.zup.realwave.sales.manager.domain.CouponCode
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId

data class UpdateCouponCommand(
    val id: PurchaseOrderId,
    val coupon: CouponCode
)
