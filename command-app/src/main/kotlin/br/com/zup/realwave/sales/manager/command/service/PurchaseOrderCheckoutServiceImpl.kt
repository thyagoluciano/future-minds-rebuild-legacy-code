package br.com.zup.realwave.sales.manager.command.service

import br.com.zup.realwave.sales.manager.domain.CustomerOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.command.CheckoutCommand
import br.com.zup.realwave.sales.manager.domain.port.PurchaseOrderCheckoutService
import br.com.zup.realwave.sales.manager.infrastructure.feign.ComClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class PurchaseOrderCheckoutServiceImpl(
    private val comClient: ComClient,
    @Value("\${com.external.module:sales-manager}") private val externalModule: String
) : PurchaseOrderCheckoutService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun checkout(purchaseOrder: PurchaseOrder, command: CheckoutCommand): CustomerOrder {
        log.debug("Checking out purchase order id={}", purchaseOrder.id)

        val request = buildCustomerOrderRequest(purchaseOrder, command)
        val response = comClient.createCustomerOrder(request)

        log.debug("Customer order created id={} for purchase order id={}", response.id, purchaseOrder.id)
        return CustomerOrder(id = response.id)
    }

    private fun buildCustomerOrderRequest(
        purchaseOrder: PurchaseOrder,
        command: CheckoutCommand
    ): ComClient.CustomerOrderRequest {
        val securityCodeMap = command.securityCodes.associateBy { it.catalogOfferItemId }

        val products = purchaseOrder.installationAttributes.map { (productTypeId, attr) ->
            ComClient.CustomerOrderProduct(
                productTypeId = productTypeId.value,
                productTypeName = productTypeId.value,
                installationAttributes = attr.attributes.mapValues { it.value as Any }
            )
        }

        val offers = purchaseOrder.items.map { item ->
            val offerItems = item.offerItems.map { offerItem ->
                val securityCode = securityCodeMap[offerItem.id]?.code
                ComClient.CustomerOrderOfferItem(
                    catalogOfferItemId = offerItem.id,
                    productTypeId = offerItem.id,
                    productId = offerItem.productId,
                    productTypeName = null,
                    price = ComClient.PriceDto(
                        amount = offerItem.price.amount,
                        currency = offerItem.price.currency,
                        scale = offerItem.price.scale
                    ),
                    recurrent = false,
                    compositionId = offerItem.id,
                    compositionName = offerItem.id,
                    userParameters = if (securityCode != null) mapOf("securityCode" to securityCode) else null
                )
            }

            ComClient.CustomerOrderOffer(
                catalogOfferId = item.catalogOfferId,
                catalogOfferType = purchaseOrder.type?.name ?: "NORMAL",
                catalogOfferName = item.catalogOfferId,
                catalogOfferDescription = item.catalogOfferId,
                price = ComClient.PriceDto(
                    amount = item.price.amount,
                    currency = item.price.currency,
                    scale = item.price.scale
                ),
                offerItems = offerItems,
                quantity = 1
            )
        }

        val paymentMethods = purchaseOrder.payment.methods.map { method ->
            ComClient.PaymentMethodDto(
                method = method.type,
                methodId = method.cardToken,
                price = method.totalValue?.let {
                    ComClient.PriceDto(amount = it.amount, currency = it.currency, scale = it.scale)
                },
                installments = method.installments
            )
        }

        val payment = ComClient.CustomerOrderPayment(
            methods = paymentMethods,
            description = purchaseOrder.payment.description
        )

        val freight = purchaseOrder.freight?.let { f ->
            f.address?.let { addr ->
                ComClient.CustomerOrderFreight(
                    address = ComClient.FreightAddress(
                        city = addr.city,
                        complement = addr.complement,
                        country = addr.country,
                        district = addr.neighborhood,
                        name = addr.street,
                        state = addr.state,
                        street = addr.street,
                        zipCode = addr.zipCode,
                        number = addr.number ?: ""
                    ),
                    price = ComClient.PriceDto(
                        amount = f.price.amount,
                        currency = f.price.currency,
                        scale = f.price.scale
                    ),
                    type = f.type ?: "NORMAL",
                    deliveryTotalTime = f.deliveryEstimateBusinessDays ?: 0
                )
            }
        }

        val mgm = purchaseOrder.mgm?.let { ComClient.Mgm(invite = it.code) }

        val coupon = purchaseOrder.coupon?.let { couponCode ->
            ComClient.CouponCode(code = couponCode.code)
        }

        return ComClient.CustomerOrderRequest(
            customerId = purchaseOrder.customer?.id ?: "",
            externalId = purchaseOrder.id.value,
            externalModule = externalModule,
            callback = purchaseOrder.callback?.url ?: "",
            type = purchaseOrder.type?.name ?: "NORMAL",
            products = products,
            offers = offers,
            payment = payment,
            mgm = mgm,
            subscriptionId = purchaseOrder.subscriptionId?.value,
            coupon = coupon,
            freight = freight
        )
    }
}
