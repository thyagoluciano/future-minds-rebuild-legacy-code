package br.com.zup.realwave.sales.manager.command.controller

import br.com.zup.realwave.sales.manager.api.PurchaseOrderCommandApi
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
import br.com.zup.realwave.sales.manager.api.request.toAddCommand
import br.com.zup.realwave.sales.manager.api.request.toCommand
import br.com.zup.realwave.sales.manager.api.request.toUpdateCommand
import br.com.zup.realwave.sales.manager.api.response.CallbackResponse
import br.com.zup.realwave.sales.manager.api.response.ChannelResponse
import br.com.zup.realwave.sales.manager.api.response.CheckoutResponse
import br.com.zup.realwave.sales.manager.api.response.CouponResponse
import br.com.zup.realwave.sales.manager.api.response.CustomerOrderInfo
import br.com.zup.realwave.sales.manager.api.response.CustomerResponse
import br.com.zup.realwave.sales.manager.api.response.DeleteInstallationAttributesResponse
import br.com.zup.realwave.sales.manager.api.response.DeleteResponse
import br.com.zup.realwave.sales.manager.api.response.CreatePurchaseOrderResponse
import br.com.zup.realwave.sales.manager.api.response.DescriptionResponse
import br.com.zup.realwave.sales.manager.api.response.FreightResponse
import br.com.zup.realwave.sales.manager.api.response.InstallationAttributesResponse
import br.com.zup.realwave.sales.manager.api.response.ItemResponse
import br.com.zup.realwave.sales.manager.api.response.MgmResponse
import br.com.zup.realwave.sales.manager.api.response.OfferItemResponse
import br.com.zup.realwave.sales.manager.api.response.OfferValidityResponse
import br.com.zup.realwave.sales.manager.api.response.OnBoardingSaleResponse
import br.com.zup.realwave.sales.manager.api.response.PaymentMethodResponse
import br.com.zup.realwave.sales.manager.api.response.PaymentResponse
import br.com.zup.realwave.sales.manager.api.response.PriceResponse
import br.com.zup.realwave.sales.manager.api.response.ProtocolResponse
import br.com.zup.realwave.sales.manager.api.response.PurchaseOrderItemResponse
import br.com.zup.realwave.sales.manager.api.response.PurchaseOrderMgmResponse
import br.com.zup.realwave.sales.manager.api.response.PurchaseOrderResponse
import br.com.zup.realwave.sales.manager.api.response.PurchaseOrderSalesForceResponse
import br.com.zup.realwave.sales.manager.api.response.PurchaseOrderTypeResponse
import br.com.zup.realwave.sales.manager.api.response.ReasonResponse
import br.com.zup.realwave.sales.manager.api.response.SalesForceResponse
import br.com.zup.realwave.sales.manager.api.response.SegmentationResponse
import br.com.zup.realwave.sales.manager.api.response.SubscriptionResponse
import br.com.zup.realwave.sales.manager.api.response.UpdateCouponResponse
import br.com.zup.realwave.sales.manager.api.response.UpdateCustomerIdResponse
import br.com.zup.realwave.sales.manager.api.response.UpdateFreightResponse
import br.com.zup.realwave.sales.manager.api.response.UpdateInstallationAttributesResponse
import br.com.zup.realwave.sales.manager.api.response.UpdateOnBoardingSaleResponse
import br.com.zup.realwave.sales.manager.api.response.UpdatePaymentResponse
import br.com.zup.realwave.sales.manager.api.response.ValidateResponse
import br.com.zup.realwave.sales.manager.command.handler.PurchaseOrderCommandHandler
import br.com.zup.realwave.sales.manager.command.handler.PurchaseOrderCouponCommandHandler
import br.com.zup.realwave.sales.manager.command.handler.PurchaseOrderCustomerCommandHandler
import br.com.zup.realwave.sales.manager.command.handler.PurchaseOrderCustomerOrderCommandHandler
import br.com.zup.realwave.sales.manager.command.handler.PurchaseOrderFreightCommandHandler
import br.com.zup.realwave.sales.manager.command.handler.PurchaseOrderInstallationAttributesCommandHandler
import br.com.zup.realwave.sales.manager.command.handler.PurchaseOrderItemCommandHandler
import br.com.zup.realwave.sales.manager.command.handler.PurchaseOrderMgmCommandHandler
import br.com.zup.realwave.sales.manager.command.handler.PurchaseOrderOnBoardingSaleCommandHandler
import br.com.zup.realwave.sales.manager.command.handler.PurchaseOrderPaymentCommandHandler
import br.com.zup.realwave.sales.manager.command.handler.PurchaseOrderProtocolCommandHandler
import br.com.zup.realwave.sales.manager.command.handler.PurchaseOrderSalesForceCommandHandler
import br.com.zup.realwave.sales.manager.command.handler.PurchaseOrderSegmentationCommandHandler
import br.com.zup.realwave.sales.manager.command.handler.PurchaseOrderSubscriptionCommandHandler
import br.com.zup.realwave.sales.manager.domain.Channel
import br.com.zup.realwave.sales.manager.domain.Freight
import br.com.zup.realwave.sales.manager.domain.Item
import br.com.zup.realwave.sales.manager.domain.ItemId
import br.com.zup.realwave.sales.manager.domain.Mgm
import br.com.zup.realwave.sales.manager.domain.Payment
import br.com.zup.realwave.sales.manager.domain.PaymentMethod
import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.SalesForce
import br.com.zup.realwave.sales.manager.domain.Segmentation
import br.com.zup.realwave.sales.manager.domain.command.DeleteInstallationAttributesCommand
import br.com.zup.realwave.sales.manager.domain.command.DeleteMgmCommand
import br.com.zup.realwave.sales.manager.domain.command.DeletePurchaseOrderCommand
import br.com.zup.realwave.sales.manager.domain.command.FindPurchaseOrderCommand
import br.com.zup.realwave.sales.manager.domain.command.RemoveSalesForceCommand
import br.com.zup.realwave.sales.manager.domain.command.UpdatePurchaseOrderTypeCommand
import br.com.zup.realwave.sales.manager.domain.command.UpdateSegmentationCommand
import br.com.zup.realwave.sales.manager.domain.command.ValidatePurchaseOrderCommand
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import jakarta.validation.Valid

@RestController
class PurchaseOrderCommandController(
    private val commandHandler: PurchaseOrderCommandHandler,
    private val couponCommandHandler: PurchaseOrderCouponCommandHandler,
    private val segmentationCommandHandler: PurchaseOrderSegmentationCommandHandler,
    private val onBoardingSaleCommandHandler: PurchaseOrderOnBoardingSaleCommandHandler,
    private val mgmCommandHandler: PurchaseOrderMgmCommandHandler,
    private val customerCommandHandler: PurchaseOrderCustomerCommandHandler,
    private val itemCommandHandler: PurchaseOrderItemCommandHandler,
    private val paymentCommandHandler: PurchaseOrderPaymentCommandHandler,
    private val installationAttributesCommandHandler: PurchaseOrderInstallationAttributesCommandHandler,
    private val customerOrderCommandHandler: PurchaseOrderCustomerOrderCommandHandler,
    private val subscriptionCommandHandler: PurchaseOrderSubscriptionCommandHandler,
    private val protocolCommandHandler: PurchaseOrderProtocolCommandHandler,
    private val salesForceCommandHandler: PurchaseOrderSalesForceCommandHandler,
    private val freightCommandHandler: PurchaseOrderFreightCommandHandler
) : PurchaseOrderCommandApi {

    private val mapper = ObjectMapper()

    override fun create(
        @Valid @RequestBody(required = false) purchaseOrderRequest: PurchaseOrderRequest?
    ): CreatePurchaseOrderResponse {
        val command = purchaseOrderRequest?.toCommand()
            ?: br.com.zup.realwave.sales.manager.domain.command.CreatePurchaseOrderCommand(
                purchaseOrderType = null,
                callback = null
            )
        val id = commandHandler.handle(command)
        return CreatePurchaseOrderResponse(id = id.value)
    }

    override fun findByPurchaseOrderId(
        @PathVariable("purchaseOrderId") purchaseOrderId: String
    ): PurchaseOrderResponse {
        val command = FindPurchaseOrderCommand(id = PurchaseOrderId(purchaseOrderId))
        val purchaseOrder = commandHandler.handle(command)
        return purchaseOrder.toResponse()
    }

    override fun createPurchaseCoupon(
        @Valid @RequestBody purchaseOrderRequest: PurchaseOrderCouponRequest
    ): CreatePurchaseOrderResponse {
        val command = purchaseOrderRequest.toCommand()
        val id = commandHandler.handle(command)
        return CreatePurchaseOrderResponse(id = id.value)
    }

    override fun segmentation(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody segmentation: JsonNode
    ): SegmentationResponse {
        val fields: Map<String, Any> = mapper.convertValue(segmentation, Map::class.java) as Map<String, Any>
        val command = UpdateSegmentationCommand(
            id = PurchaseOrderId(purchaseOrderId),
            segmentation = Segmentation(customFields = fields)
        )
        segmentationCommandHandler.handle(command)
        return SegmentationResponse(purchaseOrderId = purchaseOrderId, segmentation = segmentation)
    }

    override fun updateOnBoardingSale(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody onBoardingSale: OnBoardingSaleRequest
    ): UpdateOnBoardingSaleResponse {
        val command = onBoardingSale.toCommand(PurchaseOrderId(purchaseOrderId))
        onBoardingSaleCommandHandler.handle(command)
        return UpdateOnBoardingSaleResponse(
            purchaseOrderId = purchaseOrderId,
            id = onBoardingSale.id,
            customFields = onBoardingSale.customFields
        )
    }

    override fun updateMgm(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody mgmRequest: MgmRequest
    ): PurchaseOrderMgmResponse {
        val command = mgmRequest.toCommand(PurchaseOrderId(purchaseOrderId))
        mgmCommandHandler.handle(command)
        return PurchaseOrderMgmResponse(purchaseOrderId = purchaseOrderId, code = mgmRequest.code)
    }

    override fun deleteMgm(@PathVariable("purchaseOrderId") purchaseOrderId: String): PurchaseOrderMgmResponse {
        val command = DeleteMgmCommand(id = PurchaseOrderId(purchaseOrderId))
        mgmCommandHandler.handle(command)
        return PurchaseOrderMgmResponse(purchaseOrderId = purchaseOrderId, code = null)
    }

    override fun updateCustomerId(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody customerRequest: CustomerRequest
    ): UpdateCustomerIdResponse {
        val command = customerRequest.toCommand(PurchaseOrderId(purchaseOrderId))
        customerCommandHandler.handle(command)
        return UpdateCustomerIdResponse(purchaseOrderId = purchaseOrderId, customer = customerRequest.customer)
    }

    override fun addItem(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody itemRequest: ItemRequest
    ): PurchaseOrderItemResponse {
        val purchaseOrderIdObj = PurchaseOrderId(purchaseOrderId)
        val command = itemRequest.toAddCommand(purchaseOrderIdObj)
        itemCommandHandler.handle(command)
        return PurchaseOrderItemResponse(purchaseOrderId = purchaseOrderId, itemId = command.item.id.value)
    }

    override fun updateItem(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @PathVariable("itemId") itemId: String,
        @Valid @RequestBody itemRequest: ItemRequest
    ): PurchaseOrderItemResponse {
        val command = itemRequest.toUpdateCommand(PurchaseOrderId(purchaseOrderId), ItemId(itemId))
        itemCommandHandler.handle(command)
        return PurchaseOrderItemResponse(purchaseOrderId = purchaseOrderId, itemId = itemId)
    }

    override fun deleteItem(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @PathVariable("catalogOfferId") itemId: String
    ): PurchaseOrderItemResponse {
        val command = br.com.zup.realwave.sales.manager.domain.command.RemoveItemCommand(
            id = PurchaseOrderId(purchaseOrderId),
            itemId = ItemId(itemId)
        )
        itemCommandHandler.handle(command)
        return PurchaseOrderItemResponse(purchaseOrderId = purchaseOrderId, itemId = itemId)
    }

    override fun updatePayment(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody paymentRequest: PaymentRequest
    ): UpdatePaymentResponse {
        val command = paymentRequest.toCommand(PurchaseOrderId(purchaseOrderId))
        paymentCommandHandler.handle(command)
        return UpdatePaymentResponse(purchaseOrderId = purchaseOrderId)
    }

    override fun updateFreight(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody freightRequest: FreightRequest
    ): UpdateFreightResponse {
        val command = freightRequest.toCommand(PurchaseOrderId(purchaseOrderId))
        freightCommandHandler.handle(command)
        return UpdateFreightResponse(purchaseOrderId = purchaseOrderId)
    }

    override fun updateInstallationAttributes(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody request: InstallationAttributesRequest
    ): UpdateInstallationAttributesResponse {
        val command = request.toCommand(PurchaseOrderId(purchaseOrderId))
        installationAttributesCommandHandler.handle(command)
        return UpdateInstallationAttributesResponse(
            purchaseOrderId = purchaseOrderId,
            productTypeId = request.productTypeId,
            attributes = request.attributes
        )
    }

    override fun deleteInstallationAttributes(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @PathVariable("productTypeId") productTypeId: String
    ): DeleteInstallationAttributesResponse {
        val command = DeleteInstallationAttributesCommand(
            id = PurchaseOrderId(purchaseOrderId),
            productTypeId = br.com.zup.realwave.sales.manager.domain.ProductTypeId(productTypeId)
        )
        installationAttributesCommandHandler.handle(command)
        return DeleteInstallationAttributesResponse(purchaseOrderId = purchaseOrderId, productTypeId = productTypeId)
    }

    override fun updateCoupon(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody couponRequest: CouponRequest
    ): UpdateCouponResponse {
        val command = couponRequest.toCommand(PurchaseOrderId(purchaseOrderId))
        couponCommandHandler.handle(command)
        return UpdateCouponResponse(
            purchaseOrderId = purchaseOrderId,
            code = couponRequest.code,
            customFields = couponRequest.customFields
        )
    }

    override fun validate(@PathVariable("purchaseOrderId") purchaseOrderId: String): ValidateResponse {
        val command = ValidatePurchaseOrderCommand(id = PurchaseOrderId(purchaseOrderId))
        commandHandler.handle(command)
        return ValidateResponse(purchaseOrderId = purchaseOrderId)
    }

    override fun checkout(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @RequestBody(required = false) checkoutRequest: CheckoutRequest?
    ): CheckoutResponse {
        val channel = Channel(id = "NOT INFORMED", type = "NOT INFORMED")
        val command = checkoutRequest.toCommand(PurchaseOrderId(purchaseOrderId), channel)
        val customerOrder = commandHandler.handle(command)
        return CheckoutResponse(
            id = purchaseOrderId,
            customerOrder = CustomerOrderInfo(id = customerOrder.id)
        )
    }

    override fun delete(@PathVariable("purchaseOrderId") purchaseOrderId: String): DeleteResponse {
        val command = DeletePurchaseOrderCommand(id = PurchaseOrderId(purchaseOrderId))
        commandHandler.handle(command)
        return DeleteResponse(purchaseOrderId = purchaseOrderId)
    }

    override fun callback(@RequestBody @Valid customerOrderCallbackRequest: CustomerOrderCallbackRequest) {
        val command = customerOrderCallbackRequest.toCommand()
        customerOrderCommandHandler.handle(command)
    }

    override fun protocol(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody protocolRequest: ProtocolRequest
    ): ProtocolResponse {
        val command = protocolRequest.toCommand(PurchaseOrderId(purchaseOrderId))
        protocolCommandHandler.handle(command)
        return ProtocolResponse(purchaseOrderId = purchaseOrderId, protocol = protocolRequest.protocol)
    }

    override fun subscription(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody subscriptionRequest: SubscriptionRequest
    ): SubscriptionResponse {
        val command = subscriptionRequest.toCommand(PurchaseOrderId(purchaseOrderId))
        subscriptionCommandHandler.handle(command)
        return SubscriptionResponse(purchaseOrderId = purchaseOrderId, id = subscriptionRequest.id)
    }

    override fun purchaseOrderType(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody(required = true) purchaseOrderRequest: PurchaseOrderRequest
    ): PurchaseOrderTypeResponse {
        val purchaseOrderType = purchaseOrderRequest.type?.let {
            br.com.zup.realwave.sales.manager.domain.PurchaseOrderType.valueOf(it)
        }
        val command = UpdatePurchaseOrderTypeCommand(
            id = PurchaseOrderId(purchaseOrderId),
            type = purchaseOrderType
        )
        commandHandler.handle(command)
        return PurchaseOrderTypeResponse(purchaseOrderId = purchaseOrderId, type = purchaseOrderRequest.type)
    }

    override fun updateSalesForce(
        @PathVariable("purchaseOrderId") purchaseOrderId: String,
        @Valid @RequestBody salesForceRequest: SalesForceRequest
    ): PurchaseOrderSalesForceResponse {
        val command = salesForceRequest.toCommand(PurchaseOrderId(purchaseOrderId))
        salesForceCommandHandler.handle(command)
        return PurchaseOrderSalesForceResponse(purchaseOrderId = purchaseOrderId, salesForceId = salesForceRequest.id)
    }

    override fun deleteSalesForce(
        @PathVariable("purchaseOrderId") purchaseOrderId: String
    ): PurchaseOrderSalesForceResponse {
        val command = RemoveSalesForceCommand(id = PurchaseOrderId(purchaseOrderId))
        salesForceCommandHandler.handle(command)
        return PurchaseOrderSalesForceResponse(purchaseOrderId = purchaseOrderId)
    }

    // ─── Private mapping helpers ───────────────────────────────────────────────

    private fun PurchaseOrder.toResponse(): PurchaseOrderResponse = PurchaseOrderResponse(
        id = id.value,
        type = type?.name,
        protocol = protocol?.value,
        subscriptionId = subscriptionId?.value,
        segmentation = segmentation?.let { mapper.valueToTree(it.customFields) },
        mgm = mgm?.let { MgmResponse(code = it.code, fields = mapper.valueToTree(it.customFields)) },
        salesForce = salesForce?.let { SalesForceResponse(id = it.agentId, name = it.supervisorId) },
        onBoardingSale = onBoardingSale?.let {
            OnBoardingSaleResponse(id = null, fields = mapper.valueToTree(it.customFields))
        },
        customer = customer?.let { CustomerResponse(id = it.id) },
        coupon = coupon?.let { CouponResponse(id = it.code, fields = null) },
        totalPrice = null,
        discount = null,
        payment = payment.toResponse(),
        freight = freight?.toResponse(),
        status = status.name,
        items = items.map { it.toResponse() },
        installationAttributes = installationAttributes.map { (key, value) ->
            InstallationAttributesResponse(productTypeId = key.value, attributes = value.attributes)
        },
        channelCreate = ChannelResponse(channelCreate?.id),
        channelCheckout = ChannelResponse(channelCheckout?.id),
        callback = callback?.let { cb ->
            CallbackResponse(url = cb.url, headers = mapper.valueToTree(cb.headers))
        },
        reason = reason?.let { ReasonResponse(code = it.code, description = it.message) },
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Payment.toResponse(): PaymentResponse = PaymentResponse(
        methods = methods.map { it.toResponse() },
        description = DescriptionResponse(value = description)
    )

    private fun PaymentMethod.toResponse(): PaymentMethodResponse = PaymentMethodResponse(
        method = type,
        methodId = cardToken,
        price = totalValue?.let {
            PriceResponse(currency = it.currency, amount = it.amount.toInt(), scale = it.scale)
        },
        customFields = null,
        securityCodeInformed = false,
        installments = installments
    )

    private fun Freight.toResponse(): FreightResponse? {
        val addr = address ?: return null
        return FreightResponse(
            type = this.type ?: "",
            price = FreightResponse.FreightPriceResponse(
                currency = price.currency,
                amount = price.amount.toInt(),
                scale = price.scale
            ),
            address = FreightResponse.FreightAddressResponse(
                city = addr.city,
                complement = addr.complement ?: "",
                country = addr.country,
                district = addr.neighborhood,
                name = addr.street,
                state = addr.state,
                street = addr.street,
                number = addr.number ?: "",
                zipCode = addr.zipCode
            ),
            deliveryTotalTime = deliveryEstimateBusinessDays ?: 0
        )
    }

    private fun Item.toResponse(): ItemResponse = ItemResponse(
        id = id.value,
        catalogOfferId = catalogOfferId,
        catalogOfferType = null,
        price = PriceResponse(
            currency = price.currency,
            amount = price.amount.toInt(),
            scale = price.scale
        ),
        validity = validity?.let {
            OfferValidityResponse(period = it.period, duration = it.duration, unlimited = it.unlimited)
        } ?: OfferValidityResponse(period = null, duration = null, unlimited = false),
        customFields = null,
        offerItems = offerItems.map { oi ->
            OfferItemResponse(
                productId = oi.productId,
                catalogOfferItemId = oi.id,
                price = PriceResponse(
                    currency = oi.price.currency,
                    amount = oi.price.amount.toInt(),
                    scale = oi.price.scale
                ),
                customFields = null,
                recurrent = false,
                userParameters = null
            )
        },
        pricesPerPeriod = emptyList(),
        quantity = 1
    )

    private fun SalesForce.toResponse(): SalesForceResponse = SalesForceResponse(
        id = agentId,
        name = supervisorId
    )
}
