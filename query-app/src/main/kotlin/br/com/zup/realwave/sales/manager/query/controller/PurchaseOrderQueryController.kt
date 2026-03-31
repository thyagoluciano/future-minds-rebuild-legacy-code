package br.com.zup.realwave.sales.manager.query.controller

import br.com.zup.realwave.sales.manager.api.PurchaseOrderQueryApi
import br.com.zup.realwave.sales.manager.api.response.PurchaseOrderResponse
import br.com.zup.realwave.sales.manager.api.response.PurchaseOrderStatusResponse
import br.com.zup.realwave.sales.manager.domain.exception.PurchaseOrderNotFoundException
import br.com.zup.realwave.sales.manager.query.repository.PurchaseOrderQueryRepository
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PurchaseOrderQueryController(
    private val repository: PurchaseOrderQueryRepository
) : PurchaseOrderQueryApi {

    override fun findByPurchaseOrderId(@PathVariable purchaseOrderId: String): PurchaseOrderResponse? {
        return repository.findById(purchaseOrderId)
            ?: throw PurchaseOrderNotFoundException(purchaseOrderId)
    }

    override fun findByProtocol(@PathVariable protocol: String): PurchaseOrderResponse? {
        return repository.findByProtocol(protocol)
            ?: throw PurchaseOrderNotFoundException(protocol)
    }

    override fun getPurchaseOrderStatus(@PathVariable purchaseOrderId: String): PurchaseOrderStatusResponse? {
        return repository.getStatus(purchaseOrderId)
            ?: throw PurchaseOrderNotFoundException(purchaseOrderId)
    }

    override fun findByCustomer(
        @RequestParam(value = "customerId", required = true) customerId: String,
        @RequestParam(value = "status", required = false) status: String?,
        @RequestParam(value = "start", required = false) start: String?,
        @RequestParam(value = "end", required = false) end: String?
    ): List<PurchaseOrderResponse>? {
        return repository.findByCustomer(customerId, status, start, end)
    }
}
