package br.com.zup.realwave.sales.manager.api

import br.com.zup.realwave.sales.manager.api.response.PurchaseOrderResponse
import br.com.zup.realwave.sales.manager.api.response.PurchaseOrderStatusResponse
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus

interface PurchaseOrderQueryApi {

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}"],
        method = [RequestMethod.GET],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun findByPurchaseOrderId(@PathVariable("purchaseOrderId") purchaseOrderId: String): PurchaseOrderResponse?

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{protocol}/protocol"],
        method = [RequestMethod.GET],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun findByProtocol(@PathVariable("protocol") protocol: String): PurchaseOrderResponse?

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders/{purchaseOrderId}/status"],
        method = [RequestMethod.GET],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun getPurchaseOrderStatus(@PathVariable("purchaseOrderId") purchaseOrderId: String): PurchaseOrderStatusResponse?

    @ResponseStatus(OK)
    @ResponseBody
    @RequestMapping(
        value = ["/purchase-orders"],
        method = [RequestMethod.GET],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun findByCustomer(
        @RequestParam(value = "customerId", required = true) customerId: String,
        @RequestParam(value = "status", required = false) status: String?,
        @RequestParam(value = "start", required = false) start: String?,
        @RequestParam(value = "end", required = false) end: String?
    ): List<PurchaseOrderResponse>?
}
