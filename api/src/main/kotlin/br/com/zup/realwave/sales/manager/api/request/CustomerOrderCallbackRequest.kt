package br.com.zup.realwave.sales.manager.api.request

import br.com.zup.realwave.sales.manager.domain.CustomerOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.Reason
import br.com.zup.realwave.sales.manager.domain.command.UpdateCustomerOrderCommand
import jakarta.validation.constraints.NotBlank

data class CustomerOrderCallbackRequest(
    @field:[NotBlank] val id: String?,
    @field:[NotBlank] val externalId: String?,
    @field:[NotBlank] val status: String?,
    val steps: List<StepRequest>?,
    val reason: ReasonRequest?
)

data class StepRequest(
    val step: String?,
    val status: String?,
    val startedAt: String?,
    val endedAt: String?,
    val processed: Int?,
    val total: Int?
)

data class ReasonRequest(val code: String?, val description: String?)

fun CustomerOrderCallbackRequest.toCommand(): UpdateCustomerOrderCommand = UpdateCustomerOrderCommand(
    id = PurchaseOrderId(externalId!!),
    customerOrder = CustomerOrder(id = id!!),
    reason = reason?.let { r ->
        if (r.code != null) Reason(code = r.code, message = r.description) else null
    }
)
