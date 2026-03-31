package br.com.zup.realwave.sales.manager.api.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

data class CreatePurchaseOrderResponse(val id: String)

data class CheckoutResponse(val id: String, val customerOrder: CustomerOrderInfo?)

data class CustomerOrderInfo(
    val id: String,
    @field:[JsonInclude(JsonInclude.Include.NON_NULL)]
    val boleto: BoletoResponse? = null
)

data class BoletoResponse(val methodId: String?, val payload: JsonNode?)

data class SegmentationResponse(val purchaseOrderId: String, val segmentation: JsonNode?)

data class UpdateOnBoardingSaleResponse(
    val purchaseOrderId: String,
    val id: String?,
    val customFields: JsonNode? = ObjectMapper().createObjectNode()
)

data class PurchaseOrderMgmResponse(
    val purchaseOrderId: String,
    val code: String?,
    val customFields: JsonNode? = ObjectMapper().createObjectNode()
)

data class UpdateCustomerIdResponse(val purchaseOrderId: String, val customer: String?)

data class UpdatePaymentResponse(val purchaseOrderId: String)

data class UpdateFreightResponse(val purchaseOrderId: String)

data class UpdateInstallationAttributesResponse(
    val purchaseOrderId: String,
    val productTypeId: String?,
    val attributes: Map<String, Any>?
)

data class DeleteInstallationAttributesResponse(val purchaseOrderId: String, val productTypeId: String)

data class UpdateCouponResponse(
    val purchaseOrderId: String,
    val code: String?,
    val customFields: JsonNode? = ObjectMapper().createObjectNode()
)

data class ValidateResponse(val purchaseOrderId: String)

data class DeleteResponse(val purchaseOrderId: String)

data class ProtocolResponse(val purchaseOrderId: String, val protocol: String)

data class SubscriptionResponse(val purchaseOrderId: String, val id: String)

data class PurchaseOrderTypeResponse(val purchaseOrderId: String, val type: String? = null)

data class PurchaseOrderItemResponse(val purchaseOrderId: String, val itemId: String)

data class PurchaseOrderSalesForceResponse(val purchaseOrderId: String, val salesForceId: String? = null)
