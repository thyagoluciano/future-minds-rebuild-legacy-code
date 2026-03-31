package br.com.zup.realwave.sales.manager.domain

import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderCreated

class PurchaseOrder(id: PurchaseOrderId) : AggregateRoot<PurchaseOrderId>() {

    override var id: PurchaseOrderId = id
        internal set

    var status: PurchaseOrderStatus = PurchaseOrderStatus.OPENED
        internal set

    var type: PurchaseOrderType? = null
        internal set

    var customer: Customer? = null
        internal set

    var callback: Callback? = null
        internal set

    var channelCreate: Channel? = null
        internal set

    var channelCheckout: Channel? = null
        internal set

    val items: MutableSet<Item> = mutableSetOf()

    var payment: Payment = Payment()
        internal set

    var freight: Freight? = null
        internal set

    var coupon: CouponCode? = null
        internal set

    var customerOrder: CustomerOrder? = null
        internal set

    var protocol: Protocol? = null
        internal set

    var subscriptionId: SubscriptionId? = null
        internal set

    var segmentation: Segmentation? = null
        internal set

    var onBoardingSale: OnBoardingSale? = null
        internal set

    var mgm: Mgm? = null
        internal set

    var salesForce: SalesForce? = null
        internal set

    val installationAttributes: MutableMap<ProductTypeId, InstallationAttribute> = mutableMapOf()

    var reason: Reason? = null
        internal set

    var securityCodeInformed: List<SecurityCodeInformed> = emptyList()
        internal set

    var createdAt: String? = null
        internal set

    var updatedAt: String? = null
        internal set

    companion object {
        fun create(
            id: PurchaseOrderId,
            type: PurchaseOrderType?,
            customer: Customer?,
            callback: Callback?,
            channel: Channel?
        ): PurchaseOrder {
            val purchaseOrder = PurchaseOrder(id)
            purchaseOrder.applyChange(
                PurchaseOrderCreated(
                    id = id,
                    type = type,
                    customer = customer,
                    callback = callback,
                    channel = channel
                )
            )
            return purchaseOrder
        }
    }
}
