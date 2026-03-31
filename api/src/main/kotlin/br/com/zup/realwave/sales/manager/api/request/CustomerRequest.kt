package br.com.zup.realwave.sales.manager.api.request

import br.com.zup.realwave.sales.manager.domain.Customer
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.command.UpdateCustomerCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

const val MAX_CUSTOMER_SIZE = 255

data class CustomerRequest(@field:[Valid NotBlank Size(max = MAX_CUSTOMER_SIZE)] val customer: String?)

fun CustomerRequest.toCommand(purchaseOrderId: PurchaseOrderId): UpdateCustomerCommand = UpdateCustomerCommand(
    id = purchaseOrderId,
    customer = Customer(customer!!)
)
