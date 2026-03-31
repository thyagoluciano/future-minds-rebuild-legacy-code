package br.com.zup.realwave.sales.manager.infrastructure.eventstore

import br.com.zup.realwave.sales.manager.domain.DomainEvent
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
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderReasonStatusUpdated
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderSalesForceRemoved
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderSalesForceUpdated
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderSegmentationUpdated
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderStatusUpdated
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderSubscriptionUpdated
import br.com.zup.realwave.sales.manager.domain.event.PurchaseOrderTypeUpdated
import kotlin.reflect.KClass

object EventTypeRegistry {

    private val registry: Map<String, KClass<out DomainEvent>> = mapOf(
        "PurchaseOrderCreated" to PurchaseOrderCreated::class,
        "PurchaseOrderTypeUpdated" to PurchaseOrderTypeUpdated::class,
        "PurchaseOrderDeleted" to PurchaseOrderDeleted::class,
        "PurchaseOrderCheckedOut" to PurchaseOrderCheckedOut::class,
        "PurchaseOrderCouponCreated" to PurchaseOrderCouponCreated::class,
        "PurchaseOrderCouponUpdated" to PurchaseOrderCouponUpdated::class,
        "PurchaseOrderCustomerOrderUpdated" to PurchaseOrderCustomerOrderUpdated::class,
        "PurchaseOrderCustomerUpdated" to PurchaseOrderCustomerUpdated::class,
        "PurchaseOrderFreightUpdated" to PurchaseOrderFreightUpdated::class,
        "PurchaseOrderInstallationAttributesDeleted" to PurchaseOrderInstallationAttributesDeleted::class,
        "PurchaseOrderInstallationAttributesUpdated" to PurchaseOrderInstallationAttributesUpdated::class,
        "PurchaseOrderItemAdded" to PurchaseOrderItemAdded::class,
        "PurchaseOrderItemRemoved" to PurchaseOrderItemRemoved::class,
        "PurchaseOrderItemUpdated" to PurchaseOrderItemUpdated::class,
        "PurchaseOrderMgmDeleted" to PurchaseOrderMgmDeleted::class,
        "PurchaseOrderMgmUpdated" to PurchaseOrderMgmUpdated::class,
        "PurchaseOrderOnBoardingSaleUpdated" to PurchaseOrderOnBoardingSaleUpdated::class,
        "PurchaseOrderPaymentUpdated" to PurchaseOrderPaymentUpdated::class,
        "PurchaseOrderProtocolUpdated" to PurchaseOrderProtocolUpdated::class,
        "PurchaseOrderReasonStatusUpdated" to PurchaseOrderReasonStatusUpdated::class,
        "PurchaseOrderSalesForceRemoved" to PurchaseOrderSalesForceRemoved::class,
        "PurchaseOrderSalesForceUpdated" to PurchaseOrderSalesForceUpdated::class,
        "PurchaseOrderSegmentationUpdated" to PurchaseOrderSegmentationUpdated::class,
        "PurchaseOrderStatusUpdated" to PurchaseOrderStatusUpdated::class,
        "PurchaseOrderSubscriptionUpdated" to PurchaseOrderSubscriptionUpdated::class
    )

    fun resolve(type: String): KClass<out DomainEvent> =
        registry[type] ?: error("Unknown event type: $type")
}
