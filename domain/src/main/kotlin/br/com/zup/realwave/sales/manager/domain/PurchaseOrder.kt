package br.com.zup.realwave.sales.manager.domain

import br.com.zup.realwave.sales.manager.domain.command.AddItemCommand
import br.com.zup.realwave.sales.manager.domain.command.CheckoutCommand
import br.com.zup.realwave.sales.manager.domain.command.CreatePurchaseOrderCommand
import br.com.zup.realwave.sales.manager.domain.command.CreatePurchaseOrderCouponCommand
import br.com.zup.realwave.sales.manager.domain.command.DeleteInstallationAttributesCommand
import br.com.zup.realwave.sales.manager.domain.command.DeleteMgmCommand
import br.com.zup.realwave.sales.manager.domain.command.DeletePurchaseOrderCommand
import br.com.zup.realwave.sales.manager.domain.command.RemoveItemCommand
import br.com.zup.realwave.sales.manager.domain.command.RemoveSalesForceCommand
import br.com.zup.realwave.sales.manager.domain.command.UpdateCouponCommand
import br.com.zup.realwave.sales.manager.domain.command.UpdateCustomerCommand
import br.com.zup.realwave.sales.manager.domain.command.UpdateCustomerOrderCommand
import br.com.zup.realwave.sales.manager.domain.command.UpdateFreightCommand
import br.com.zup.realwave.sales.manager.domain.command.UpdateInstallationAttributesCommand
import br.com.zup.realwave.sales.manager.domain.command.UpdateItemCommand
import br.com.zup.realwave.sales.manager.domain.command.UpdateMgmCommand
import br.com.zup.realwave.sales.manager.domain.command.UpdateOnBoardingSaleCommand
import br.com.zup.realwave.sales.manager.domain.command.UpdatePaymentCommand
import br.com.zup.realwave.sales.manager.domain.command.UpdateProtocolCommand
import br.com.zup.realwave.sales.manager.domain.command.UpdatePurchaseOrderTypeCommand
import br.com.zup.realwave.sales.manager.domain.command.UpdateSalesForceCommand
import br.com.zup.realwave.sales.manager.domain.command.UpdateSegmentationCommand
import br.com.zup.realwave.sales.manager.domain.command.UpdateSubscriptionCommand
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderCheckedOut
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderCouponCreated
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderCouponUpdated
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderCreated
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderCustomerOrderUpdated
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderCustomerUpdated
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderDeleted
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderFreightUpdated
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderInstallationAttributesDeleted
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderInstallationAttributesUpdated
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderItemAdded
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderItemRemoved
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderItemUpdated
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderMgmDeleted
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderMgmUpdated
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderOnBoardingSaleUpdated
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderPaymentUpdated
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderProtocolUpdated
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderSalesForceRemoved
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderSalesForceUpdated
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderSegmentationUpdated
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderSubscriptionUpdated
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderTypeUpdated
import br.com.zup.realwave.sales.manager.domain.exception.InvalidStatusTransitionException

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

    var items: MutableSet<Item> = mutableSetOf()
        internal set

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

    var channelCreate: Channel? = null
        internal set

    var channelCheckout: Channel? = null
        internal set

    var segmentation: Segmentation? = null
        internal set

    var onBoardingSale: OnBoardingSale? = null
        internal set

    var mgm: Mgm? = null
        internal set

    var salesForce: SalesForce? = null
        internal set

    var installationAttributes: MutableMap<ProductTypeId, InstallationAttribute> = mutableMapOf()
        internal set

    var reason: Reason? = null
        internal set

    var securityCodeInformed: List<SecurityCodeInformed> = emptyList()
        internal set

    var createdAt: String? = null
        internal set

    var updatedAt: String? = null
        internal set

    // ─── Status transition validation ──────────────────────────────────────────

    private fun validateTransition(to: PurchaseOrderStatus) {
        val allowed = when (status) {
            PurchaseOrderStatus.OPENED -> setOf(PurchaseOrderStatus.CHECKED_OUT, PurchaseOrderStatus.DELETED)
            PurchaseOrderStatus.CHECKED_OUT -> setOf(
                PurchaseOrderStatus.COMPLETED,
                PurchaseOrderStatus.FAILED,
                PurchaseOrderStatus.CANCELED
            )
            else -> emptySet()
        }
        if (to !in allowed) {
            throw InvalidStatusTransitionException(status.name, to.name)
        }
    }

    // ─── Business methods ───────────────────────────────────────────────────────

    fun addItem(command: AddItemCommand) {
        applyChange(PurchaseOrderItemAdded(id = command.id, item = command.item))
    }

    fun removeItem(command: RemoveItemCommand) {
        applyChange(PurchaseOrderItemRemoved(id = command.id, itemId = command.itemId))
    }

    fun updateItem(command: UpdateItemCommand) {
        applyChange(PurchaseOrderItemUpdated(id = command.id, item = command.item))
    }

    fun updatePayment(command: UpdatePaymentCommand) {
        applyChange(PurchaseOrderPaymentUpdated(id = command.id, payment = command.payment))
    }

    fun updateFreight(command: UpdateFreightCommand) {
        applyChange(PurchaseOrderFreightUpdated(id = command.id, freight = command.freight))
    }

    fun updateCoupon(command: UpdateCouponCommand) {
        applyChange(PurchaseOrderCouponUpdated(id = command.id, coupon = command.coupon))
    }

    fun updateCustomer(command: UpdateCustomerCommand) {
        applyChange(PurchaseOrderCustomerUpdated(id = command.id, customer = command.customer))
    }

    fun updateProtocol(command: UpdateProtocolCommand) {
        applyChange(PurchaseOrderProtocolUpdated(id = command.id, protocol = command.protocol))
    }

    fun updateSubscription(command: UpdateSubscriptionCommand) {
        applyChange(PurchaseOrderSubscriptionUpdated(id = command.id, subscriptionId = command.subscriptionId))
    }

    fun updateSegmentation(command: UpdateSegmentationCommand) {
        applyChange(PurchaseOrderSegmentationUpdated(id = command.id, segmentation = command.segmentation))
    }

    fun updateOnBoardingSale(command: UpdateOnBoardingSaleCommand) {
        applyChange(PurchaseOrderOnBoardingSaleUpdated(id = command.id, onBoardingSale = command.onBoardingSale))
    }

    fun updateMgm(command: UpdateMgmCommand) {
        applyChange(PurchaseOrderMgmUpdated(id = command.id, mgm = command.mgm))
    }

    fun deleteMgm(command: DeleteMgmCommand) {
        applyChange(PurchaseOrderMgmDeleted(id = command.id))
    }

    fun updateSalesForce(command: UpdateSalesForceCommand) {
        applyChange(PurchaseOrderSalesForceUpdated(id = command.id, salesForce = command.salesForce))
    }

    fun removeSalesForce(command: RemoveSalesForceCommand) {
        applyChange(PurchaseOrderSalesForceRemoved(id = command.id))
    }

    fun updateInstallationAttributes(command: UpdateInstallationAttributesCommand) {
        applyChange(
            PurchaseOrderInstallationAttributesUpdated(
                id = command.id,
                productTypeId = command.installationAttribute.productTypeId,
                installationAttribute = command.installationAttribute
            )
        )
    }

    fun deleteInstallationAttributes(command: DeleteInstallationAttributesCommand) {
        applyChange(
            PurchaseOrderInstallationAttributesDeleted(
                id = command.id,
                productTypeId = command.productTypeId
            )
        )
    }

    fun updateCustomerOrder(command: UpdateCustomerOrderCommand) {
        applyChange(
            PurchaseOrderCustomerOrderUpdated(
                id = command.id,
                customerOrder = command.customerOrder,
                status = null,
                channel = null
            )
        )
    }

    fun updateType(command: UpdatePurchaseOrderTypeCommand) {
        applyChange(PurchaseOrderTypeUpdated(id = command.id, type = command.type))
    }

    fun checkout(command: CheckoutCommand, customerOrder: CustomerOrder) {
        validateTransition(PurchaseOrderStatus.CHECKED_OUT)
        applyChange(
            PurchaseOrderCheckedOut(
                id = command.id,
                customerOrder = customerOrder,
                channel = command.channel,
                securityCodes = command.securityCodes
            )
        )
    }

    fun delete(command: DeletePurchaseOrderCommand) {
        validateTransition(PurchaseOrderStatus.DELETED)
        applyChange(PurchaseOrderDeleted(id = command.id))
    }

    // ─── Companion object ──────────────────────────────────────────────────────

    companion object {

        fun create(command: CreatePurchaseOrderCommand): PurchaseOrder {
            val purchaseOrder = empty(command.id)
            purchaseOrder.applyChange(
                PurchaseOrderCreated(
                    id = command.id,
                    type = command.purchaseOrderType,
                    customer = command.customer,
                    callback = command.callback,
                    channel = null
                )
            )
            return purchaseOrder
        }

        fun createWithCoupon(command: CreatePurchaseOrderCouponCommand): PurchaseOrder {
            val purchaseOrder = empty(command.id)
            purchaseOrder.applyChange(
                PurchaseOrderCouponCreated(
                    id = command.id,
                    type = command.purchaseOrderType,
                    customer = command.customer,
                    callback = command.callback,
                    channel = null,
                    coupon = command.couponCode
                )
            )
            return purchaseOrder
        }

        fun empty(id: PurchaseOrderId): PurchaseOrder = PurchaseOrder(id)
    }
}
