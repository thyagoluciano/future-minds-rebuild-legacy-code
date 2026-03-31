package br.com.zup.realwave.sales.manager.api

import br.com.zup.realwave.sales.manager.api.request.CheckoutRequest
import br.com.zup.realwave.sales.manager.api.request.CouponRequest
import br.com.zup.realwave.sales.manager.api.request.CustomerOrderCallbackRequest
import br.com.zup.realwave.sales.manager.api.request.CustomerRequest
import br.com.zup.realwave.sales.manager.api.request.FreightRequest
import br.com.zup.realwave.sales.manager.api.request.InstallationAttributesRequest
import br.com.zup.realwave.sales.manager.api.request.ItemRequest
import br.com.zup.realwave.sales.manager.api.request.MgmRequest
import br.com.zup.realwave.sales.manager.api.request.OnBoardingSaleRequest
import br.com.zup.realwave.sales.manager.api.request.PaymentRequest
import br.com.zup.realwave.sales.manager.api.request.ProtocolRequest
import br.com.zup.realwave.sales.manager.api.request.PurchaseOrderCouponRequest
import br.com.zup.realwave.sales.manager.api.request.PurchaseOrderRequest
import br.com.zup.realwave.sales.manager.api.request.SalesForceRequest
import br.com.zup.realwave.sales.manager.api.request.SubscriptionRequest
import br.com.zup.realwave.sales.manager.api.response.CheckoutResponse
import br.com.zup.realwave.sales.manager.api.response.CreatePurchaseOrderResponse
import br.com.zup.realwave.sales.manager.api.response.DeleteInstallationAttributesResponse
import br.com.zup.realwave.sales.manager.api.response.DeleteResponse
import br.com.zup.realwave.sales.manager.api.response.ProtocolResponse
import br.com.zup.realwave.sales.manager.api.response.PurchaseOrderItemResponse
import br.com.zup.realwave.sales.manager.api.response.PurchaseOrderMgmResponse
import br.com.zup.realwave.sales.manager.api.response.PurchaseOrderResponse
import br.com.zup.realwave.sales.manager.api.response.PurchaseOrderSalesForceResponse
import br.com.zup.realwave.sales.manager.api.response.PurchaseOrderTypeResponse
import br.com.zup.realwave.sales.manager.api.response.SegmentationResponse
import br.com.zup.realwave.sales.manager.api.response.SubscriptionResponse
import br.com.zup.realwave.sales.manager.api.response.UpdateCouponResponse
import br.com.zup.realwave.sales.manager.api.response.UpdateCustomerIdResponse
import br.com.zup.realwave.sales.manager.api.response.UpdateFreightResponse
import br.com.zup.realwave.sales.manager.api.response.UpdateInstallationAttributesResponse
import br.com.zup.realwave.sales.manager.api.response.UpdateOnBoardingSaleResponse
import br.com.zup.realwave.sales.manager.api.response.UpdatePaymentResponse
import br.com.zup.realwave.sales.manager.api.response.ValidateResponse
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import jakarta.validation.Valid

interface PurchaseOrderCommandApi {

    @ResponseStatus(CREATED)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders"],
        method = [RequestMethod.POST],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun create(
        @Valid @RequestBody(required = false) purchaseOrderRequest: PurchaseOrderRequest?
    ): CreatePurchaseOrderResponse

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}"],
        method = [RequestMethod.GET],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun findByPurchaseOrderId(
        @PathVariable("purchaseOrderId") purchaseOrderId: String
    ): PurchaseOrderResponse

    @ResponseStatus(CREATED)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/coupon"],
        method = [RequestMethod.POST],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun createPurchaseCoupon(
        @Valid @RequestBody purchaseOrderRequest: PurchaseOrderCouponRequest
    ): CreatePurchaseOrderResponse

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}/segmentation"],
        method = [RequestMethod.PUT],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun segmentation(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody segmentation: JsonNode
    ): SegmentationResponse

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}/onboarding-sale"],
        method = [RequestMethod.PUT],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun updateOnBoardingSale(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody onBoardingSale: OnBoardingSaleRequest
    ): UpdateOnBoardingSaleResponse

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}/mgm"],
        method = [RequestMethod.PUT],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun updateMgm(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody mgmRequest: MgmRequest
    ): PurchaseOrderMgmResponse

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}/mgm"],
        method = [RequestMethod.DELETE],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun deleteMgm(@PathVariable("purchaseOrderId") purchaseOrderId: String): PurchaseOrderMgmResponse

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}/customer"],
        method = [RequestMethod.PUT],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun updateCustomerId(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody customerRequest: CustomerRequest
    ): UpdateCustomerIdResponse

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}/items"],
        method = [RequestMethod.POST],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun addItem(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody itemRequest: ItemRequest
    ): PurchaseOrderItemResponse

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}/items/{itemId}"],
        method = [RequestMethod.PUT],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun updateItem(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @PathVariable("itemId") itemId: String,
        @Valid @RequestBody itemRequest: ItemRequest
    ): PurchaseOrderItemResponse

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}/items/{catalogOfferId}"],
        method = [RequestMethod.DELETE],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun deleteItem(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @PathVariable("catalogOfferId") itemId: String
    ): PurchaseOrderItemResponse

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}/payment"],
        method = [RequestMethod.PUT],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun updatePayment(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody paymentRequest: PaymentRequest
    ): UpdatePaymentResponse

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}/freight"],
        method = [RequestMethod.PUT],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun updateFreight(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody freightRequest: FreightRequest
    ): UpdateFreightResponse

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}/installation-attributes"],
        method = [RequestMethod.PUT],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun updateInstallationAttributes(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody request: InstallationAttributesRequest
    ): UpdateInstallationAttributesResponse

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}/installation-attributes/{productTypeId}"],
        method = [RequestMethod.DELETE],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun deleteInstallationAttributes(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @PathVariable("productTypeId") productTypeId: String
    ): DeleteInstallationAttributesResponse

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}/coupon"],
        method = [RequestMethod.PUT],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun updateCoupon(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody couponRequest: CouponRequest
    ): UpdateCouponResponse

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}/validation"],
        method = [RequestMethod.GET],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun validate(@PathVariable("purchaseOrderId") purchaseOrderId: String): ValidateResponse

    @ResponseStatus(CREATED)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}/checkout"],
        method = [RequestMethod.POST],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun checkout(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @RequestBody(required = false) checkoutRequest: CheckoutRequest?
    ): CheckoutResponse

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}"],
        method = [RequestMethod.DELETE],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun delete(@PathVariable("purchaseOrderId") purchaseOrderId: String): DeleteResponse

    @ResponseStatus(NO_CONTENT)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/callback"],
        method = [RequestMethod.POST],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun callback(@RequestBody @Valid customerOrderCallbackRequest: CustomerOrderCallbackRequest)

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}/protocol"],
        method = [RequestMethod.PUT],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun protocol(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody protocolRequest: ProtocolRequest
    ): ProtocolResponse

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}/subscription"],
        method = [RequestMethod.PUT],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun subscription(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody subscriptionRequest: SubscriptionRequest
    ): SubscriptionResponse

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}/type"],
        method = [RequestMethod.PUT],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun purchaseOrderType(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody(required = true) purchaseOrderRequest: PurchaseOrderRequest
    ): PurchaseOrderTypeResponse

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}/sales-force"],
        method = [RequestMethod.PUT],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun updateSalesForce(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody salesForceRequest: SalesForceRequest
    ): PurchaseOrderSalesForceResponse

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}/sales-force"],
        method = [RequestMethod.DELETE],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun deleteSalesForce(@PathVariable("purchaseOrderId") purchaseOrderId: String): PurchaseOrderSalesForceResponse
}
