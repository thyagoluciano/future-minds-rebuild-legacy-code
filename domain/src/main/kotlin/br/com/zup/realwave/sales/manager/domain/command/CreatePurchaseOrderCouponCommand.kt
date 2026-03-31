package br.com.zup.realwave.sales.manager.domain.command

import br.com.zup.realwave.sales.manager.domain.Callback
import br.com.zup.realwave.sales.manager.domain.CouponCode
import br.com.zup.realwave.sales.manager.domain.Customer
import br.com.zup.realwave.sales.manager.domain.ProductTypeId
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderType

data class CreatePurchaseOrderCouponCommand(
    val purchaseOrderType: PurchaseOrderType = PurchaseOrderType.COUPON,
    val id: PurchaseOrderId = PurchaseOrderId(),
    val couponCode: CouponCode,
    val customer: Customer,
    val productTypeId: ProductTypeId,
    val callback: Callback? = null
)
