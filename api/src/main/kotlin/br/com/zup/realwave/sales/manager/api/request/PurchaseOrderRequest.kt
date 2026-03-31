package br.com.zup.realwave.sales.manager.api.request

import br.com.zup.realwave.sales.manager.api.request.validation.PurchaseOrderTypeValidation
import br.com.zup.realwave.sales.manager.domain.Callback
import br.com.zup.realwave.sales.manager.domain.Customer
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderType
import br.com.zup.realwave.sales.manager.domain.command.CreatePurchaseOrderCommand

@PurchaseOrderTypeValidation
data class PurchaseOrderRequest(
    val type: String? = null,
    val customer: String? = null,
    val callback: CallbackRequest? = null
)

fun PurchaseOrderRequest.toCommand(): CreatePurchaseOrderCommand = CreatePurchaseOrderCommand(
    purchaseOrderType = if (type == null) null else PurchaseOrderType.valueOf(type),
    customer = if (customer == null) null else Customer(customer),
    callback = callback?.toCommand()
)

fun CallbackRequest.toCommand(): Callback = Callback(
    url = url,
    headers = if (headers != null) {
        headers.fields().asSequence().associate { it.key to it.value.asText() }
    } else emptyMap()
)
