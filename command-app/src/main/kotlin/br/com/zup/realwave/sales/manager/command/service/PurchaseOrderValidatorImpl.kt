package br.com.zup.realwave.sales.manager.command.service

import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderType
import br.com.zup.realwave.sales.manager.domain.port.PurchaseOrderValidator
import br.com.zup.realwave.sales.manager.infrastructure.feign.CimClient
import br.com.zup.realwave.sales.manager.infrastructure.feign.CmsClient
import br.com.zup.realwave.sales.manager.infrastructure.feign.CouponClient
import br.com.zup.realwave.sales.manager.infrastructure.feign.MgmClient
import br.com.zup.realwave.sales.manager.infrastructure.feign.PcmClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PurchaseOrderValidatorImpl(
    private val cmsClient: CmsClient,
    private val pcmClient: PcmClient,
    private val cimClient: CimClient,
    private val couponClient: CouponClient,
    private val mgmClient: MgmClient
) : PurchaseOrderValidator {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun validate(purchaseOrder: PurchaseOrder) {
        log.debug("Validating purchase order id={}", purchaseOrder.id)

        validateCustomer(purchaseOrder)

        when (purchaseOrder.type) {
            PurchaseOrderType.JOIN -> validateJoin(purchaseOrder)
            PurchaseOrderType.BUY -> validateBuy(purchaseOrder)
            PurchaseOrderType.CHANGE -> validateChange(purchaseOrder)
            PurchaseOrderType.COUPON -> validateCoupon(purchaseOrder)
            PurchaseOrderType.NORMAL,
            PurchaseOrderType.ONBOARDING,
            null -> validateOffers(purchaseOrder)
        }
    }

    private fun validateCustomer(purchaseOrder: PurchaseOrder) {
        purchaseOrder.customer?.let { customer ->
            log.debug("Validating customer id={}", customer.id)
            cimClient.findById(customer.id)
        }
    }

    private fun validateJoin(purchaseOrder: PurchaseOrder) {
        purchaseOrder.mgm?.let { mgm ->
            log.debug("Validating mgm code={}", mgm.code)
            mgmClient.validate(mgm.code)
        }
        validateCouponIfPresent(purchaseOrder)
        validateOffers(purchaseOrder)
    }

    private fun validateBuy(purchaseOrder: PurchaseOrder) {
        validateCouponIfPresent(purchaseOrder)
        validateOffers(purchaseOrder)
        validateItemsProductId(purchaseOrder)
    }

    private fun validateChange(purchaseOrder: PurchaseOrder) {
        validateOffers(purchaseOrder)
        validateItemsProductId(purchaseOrder)
    }

    private fun validateCoupon(purchaseOrder: PurchaseOrder) {
        if (purchaseOrder.customer != null && purchaseOrder.coupon != null) {
            log.debug("Validating coupon code={} for customer={}", purchaseOrder.coupon!!.code, purchaseOrder.customer!!.id)
            couponClient.validateCoupon(purchaseOrder.coupon!!.code, purchaseOrder.customer!!.id)
        }
    }

    private fun validateCouponIfPresent(purchaseOrder: PurchaseOrder) {
        purchaseOrder.coupon?.let { coupon ->
            purchaseOrder.customer?.let { customer ->
                log.debug("Validating coupon code={} for customer={}", coupon.code, customer.id)
                couponClient.validateCoupon(coupon.code, customer.id)
            }
        }
    }

    private fun validateOffers(purchaseOrder: PurchaseOrder) {
        if (purchaseOrder.items.isEmpty()) return

        val validateItems = purchaseOrder.items.map { item ->
            CmsClient.OfferValidateItem(
                id = item.catalogOfferId,
                type = "OFFER",
                items = item.offerItems.map { offerItem ->
                    CmsClient.OfferValidateItemDetail(
                        id = offerItem.id,
                        price = CmsClient.PriceRepresentation(
                            amount = offerItem.price.amount,
                            currency = offerItem.price.currency,
                            scale = offerItem.price.scale
                        ),
                        recurrent = false
                    )
                }
            )
        }

        val request = CmsClient.OfferValidateRequest(items = validateItems)
        val response = cmsClient.validateOffers(request)

        if (response.errors.isNotEmpty()) {
            log.warn("Offer validation errors: {}", response.errors)
        }
    }

    private fun validateItemsProductId(purchaseOrder: PurchaseOrder) {
        purchaseOrder.customer?.let { customer ->
            purchaseOrder.items.flatMap { it.offerItems }.forEach { offerItem ->
                offerItem.productId?.let { productId ->
                    log.debug("Validating product id={} for customer id={}", productId, customer.id)
                    cimClient.findByCustomerIdAndProductId(customer.id, productId)
                }
            }
        }
    }
}
