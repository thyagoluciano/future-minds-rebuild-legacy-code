package br.com.zup.realwave.sales.manager.api.request

import br.com.zup.realwave.sales.manager.domain.CouponCode
import br.com.zup.realwave.sales.manager.domain.Customer
import br.com.zup.realwave.sales.manager.domain.ProductTypeId
import br.com.zup.realwave.sales.manager.domain.command.CreatePurchaseOrderCouponCommand
import jakarta.validation.constraints.NotBlank

data class PurchaseOrderCouponRequest(
    @field:NotBlank val couponCode: String?,
    @field:NotBlank val productId: String?,
    @field:NotBlank val customerId: String?,
    val callback: CallbackRequest? = null
)

fun PurchaseOrderCouponRequest.toCommand(): CreatePurchaseOrderCouponCommand = CreatePurchaseOrderCouponCommand(
    couponCode = CouponCode(couponCode!!),
    customer = Customer(customerId!!),
    productTypeId = ProductTypeId(productId!!),
    callback = callback?.toCommand()
)
