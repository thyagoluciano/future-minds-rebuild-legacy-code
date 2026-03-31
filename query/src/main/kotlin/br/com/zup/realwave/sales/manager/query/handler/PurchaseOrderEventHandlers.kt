package br.com.zup.realwave.sales.manager.query.handler

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
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class PurchaseOrderEventHandlers(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper
) {

    fun dispatch(event: DomainEvent) {
        when (event) {
            is PurchaseOrderCreated -> on(event)
            is PurchaseOrderTypeUpdated -> on(event)
            is PurchaseOrderDeleted -> on(event)
            is PurchaseOrderItemAdded -> on(event)
            is PurchaseOrderItemRemoved -> on(event)
            is PurchaseOrderItemUpdated -> on(event)
            is PurchaseOrderPaymentUpdated -> on(event)
            is PurchaseOrderFreightUpdated -> on(event)
            is PurchaseOrderCouponUpdated -> on(event)
            is PurchaseOrderCustomerUpdated -> on(event)
            is PurchaseOrderProtocolUpdated -> on(event)
            is PurchaseOrderSubscriptionUpdated -> on(event)
            is PurchaseOrderSegmentationUpdated -> on(event)
            is PurchaseOrderOnBoardingSaleUpdated -> on(event)
            is PurchaseOrderMgmUpdated -> on(event)
            is PurchaseOrderMgmDeleted -> on(event)
            is PurchaseOrderSalesForceUpdated -> on(event)
            is PurchaseOrderSalesForceRemoved -> on(event)
            is PurchaseOrderInstallationAttributesUpdated -> on(event)
            is PurchaseOrderInstallationAttributesDeleted -> on(event)
            is PurchaseOrderCustomerOrderUpdated -> on(event)
            is PurchaseOrderStatusUpdated -> on(event)
            is PurchaseOrderReasonStatusUpdated -> on(event)
            is PurchaseOrderCheckedOut -> on(event)
            is PurchaseOrderCouponCreated -> on(event)
        }
    }

    fun on(event: PurchaseOrderCreated) {
        jdbcTemplate.update(
            """INSERT INTO purchase_order (id, status, type, customer, callback, channel_create, created, updated, version)
               VALUES (?, ?, ?, ?, ?::jsonb, ?::jsonb, now(), now(), 1)""",
            event.id.value,
            "OPENED",
            event.type?.name,
            event.customer?.id,
            toJson(event.callback),
            toJson(event.channel)
        )
    }

    fun on(event: PurchaseOrderTypeUpdated) {
        jdbcTemplate.update(
            "UPDATE purchase_order SET type = ?, updated = now() WHERE id = ?",
            event.type?.name,
            event.id.value
        )
    }

    fun on(event: PurchaseOrderDeleted) {
        jdbcTemplate.update(
            "UPDATE purchase_order SET status = 'DELETED', updated = now() WHERE id = ?",
            event.id.value
        )
    }

    fun on(event: PurchaseOrderItemAdded) {
        val item = event.item
        jdbcTemplate.update(
            """INSERT INTO order_item (id, purchase_order_id, catalog_offer_id,
               price_currency, price_amount, price_scale,
               validity_period, validity_duration, validity_unlimited,
               offer_items, prices_per_period, created)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, now())""",
            item.id.value,
            event.id.value,
            item.catalogOfferId,
            item.price.currency,
            item.price.amount,
            item.price.scale,
            item.validity?.period,
            item.validity?.duration,
            item.validity?.unlimited ?: false,
            toJson(item.offerItems),
            toJson(item.pricesPerPeriod)
        )
    }

    fun on(event: PurchaseOrderItemRemoved) {
        jdbcTemplate.update(
            "DELETE FROM order_item WHERE purchase_order_id = ? AND id = ?",
            event.id.value,
            event.itemId.value
        )
    }

    fun on(event: PurchaseOrderItemUpdated) {
        val item = event.item
        jdbcTemplate.update(
            """UPDATE order_item
               SET catalog_offer_id = ?, offer_items = ?::jsonb, updated = now(),
                   price_currency = ?, price_amount = ?, price_scale = ?,
                   validity_period = ?, validity_duration = ?, validity_unlimited = ?,
                   prices_per_period = ?::jsonb
               WHERE purchase_order_id = ? AND id = ?""",
            item.catalogOfferId,
            toJson(item.offerItems),
            item.price.currency,
            item.price.amount,
            item.price.scale,
            item.validity?.period,
            item.validity?.duration,
            item.validity?.unlimited ?: false,
            toJson(item.pricesPerPeriod),
            event.id.value,
            item.id.value
        )
    }

    fun on(event: PurchaseOrderPaymentUpdated) {
        val purchaseOrderId = event.id.value
        // Update payment description on purchase_order
        jdbcTemplate.update(
            "UPDATE purchase_order SET payment_description = ?, updated = now() WHERE id = ?",
            event.payment.description,
            purchaseOrderId
        )
        // UPSERT payment methods: delete existing then insert new
        jdbcTemplate.update("DELETE FROM payment WHERE purchase_order_id = ?", purchaseOrderId)
        event.payment.methods.forEachIndexed { index, method ->
            jdbcTemplate.update(
                """INSERT INTO payment (purchase_order_id, payment_method, payment_method_id,
                   price_currency, price_amount, price_scale, installments, payment_order, created)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, now())""",
                purchaseOrderId,
                method.type,
                method.cardToken,
                method.totalValue?.currency,
                method.totalValue?.amount,
                method.totalValue?.scale,
                method.installments,
                index
            )
        }
    }

    fun on(event: PurchaseOrderFreightUpdated) {
        val purchaseOrderId = event.id.value
        val freight = event.freight
        // UPSERT freight: delete existing then insert new
        jdbcTemplate.update("DELETE FROM freight WHERE purchase_order_id = ?", purchaseOrderId)
        jdbcTemplate.update(
            """INSERT INTO freight (purchase_order_id, address, type, delivery_total_time,
               price_currency, price_amount, price_scale, created)
               VALUES (?, ?::jsonb, ?, ?, ?, ?, ?, now())""",
            purchaseOrderId,
            toJson(freight.address),
            freight.type,
            freight.deliveryEstimateBusinessDays,
            freight.price.currency,
            freight.price.amount,
            freight.price.scale
        )
    }

    fun on(event: PurchaseOrderCouponUpdated) {
        jdbcTemplate.update(
            "UPDATE purchase_order SET coupon_code = ?, updated = now() WHERE id = ?",
            event.coupon.code,
            event.id.value
        )
    }

    fun on(event: PurchaseOrderCustomerUpdated) {
        jdbcTemplate.update(
            "UPDATE purchase_order SET customer = ?, updated = now() WHERE id = ?",
            event.customer.id,
            event.id.value
        )
    }

    fun on(event: PurchaseOrderProtocolUpdated) {
        jdbcTemplate.update(
            "UPDATE purchase_order SET protocol = ?, updated = now() WHERE id = ?",
            event.protocol.value,
            event.id.value
        )
    }

    fun on(event: PurchaseOrderSubscriptionUpdated) {
        jdbcTemplate.update(
            "UPDATE purchase_order SET subscription_id = ?, updated = now() WHERE id = ?",
            event.subscriptionId.value,
            event.id.value
        )
    }

    fun on(event: PurchaseOrderSegmentationUpdated) {
        jdbcTemplate.update(
            "UPDATE purchase_order SET segmentation = ?::jsonb, updated = now() WHERE id = ?",
            toJson(event.segmentation.customFields),
            event.id.value
        )
    }

    fun on(event: PurchaseOrderOnBoardingSaleUpdated) {
        jdbcTemplate.update(
            """UPDATE purchase_order
               SET on_boarding_sale_offer_id = ?, on_boarding_sale_custom_fields = ?::jsonb, updated = now()
               WHERE id = ?""",
            null,
            toJson(event.onBoardingSale.customFields),
            event.id.value
        )
    }

    fun on(event: PurchaseOrderMgmUpdated) {
        jdbcTemplate.update(
            "UPDATE purchase_order SET mgm_code = ?, mgm_custom_fields = ?::jsonb, updated = now() WHERE id = ?",
            event.mgm.code,
            toJson(event.mgm.customFields),
            event.id.value
        )
    }

    fun on(event: PurchaseOrderMgmDeleted) {
        jdbcTemplate.update(
            "UPDATE purchase_order SET mgm_code = NULL, mgm_custom_fields = NULL, updated = now() WHERE id = ?",
            event.id.value
        )
    }

    fun on(event: PurchaseOrderSalesForceUpdated) {
        jdbcTemplate.update(
            "UPDATE purchase_order SET sales_force_id = ?, sales_force_name = ?, updated = now() WHERE id = ?",
            event.salesForce.agentId,
            event.salesForce.supervisorId,
            event.id.value
        )
    }

    fun on(event: PurchaseOrderSalesForceRemoved) {
        jdbcTemplate.update(
            "UPDATE purchase_order SET sales_force_id = NULL, sales_force_name = NULL, updated = now() WHERE id = ?",
            event.id.value
        )
    }

    fun on(event: PurchaseOrderInstallationAttributesUpdated) {
        val purchaseOrderId = event.id.value
        val attr = event.installationAttribute
        val productTypeId = attr.productTypeId.value
        val attributesJson = toJson(attr.attributes)
        // UPSERT: try update first, then insert
        val updated = jdbcTemplate.update(
            """UPDATE installation_attributes
               SET attributes = ?::jsonb, updated = now()
               WHERE purchase_order_id = ? AND product_type_id = ?""",
            attributesJson, purchaseOrderId, productTypeId
        )
        if (updated == 0) {
            jdbcTemplate.update(
                """INSERT INTO installation_attributes (purchase_order_id, product_type_id, attributes, created)
                   VALUES (?, ?, ?::jsonb, now())""",
                purchaseOrderId, productTypeId, attributesJson
            )
        }
    }

    fun on(event: PurchaseOrderInstallationAttributesDeleted) {
        jdbcTemplate.update(
            "DELETE FROM installation_attributes WHERE purchase_order_id = ? AND product_type_id = ?",
            event.id.value,
            event.productTypeId.value
        )
    }

    fun on(event: PurchaseOrderCustomerOrderUpdated) {
        val purchaseOrderId = event.id.value
        val co = event.customerOrder
        jdbcTemplate.update(
            """INSERT INTO customer_order (purchase_order_id, customer_order_id, status, steps)
               VALUES (?, ?, ?, ?::jsonb)
               ON CONFLICT (purchase_order_id)
               DO UPDATE SET customer_order_id = ?, status = ?, steps = ?::jsonb""",
            purchaseOrderId, co.id, null, null,
            co.id, null, null
        )
        val statusName = event.status?.name
        val channelJson = event.channel?.let { toJson(it) }
        if (statusName != null || channelJson != null) {
            jdbcTemplate.update(
                """UPDATE purchase_order
                   SET status = COALESCE(?, status),
                       channel_checkout = COALESCE(?::jsonb, channel_checkout),
                       updated = now()
                   WHERE id = ?""",
                statusName as Any?,
                channelJson as Any?,
                purchaseOrderId
            )
        } else {
            jdbcTemplate.update(
                "UPDATE purchase_order SET updated = now() WHERE id = ?",
                purchaseOrderId
            )
        }
    }

    fun on(event: PurchaseOrderStatusUpdated) {
        jdbcTemplate.update(
            "UPDATE purchase_order SET status = ?, reason = ?::jsonb, updated = now() WHERE id = ?",
            event.status.name,
            toJson(event.reason),
            event.id.value
        )
    }

    fun on(event: PurchaseOrderReasonStatusUpdated) {
        jdbcTemplate.update(
            "UPDATE purchase_order SET reason = ?::jsonb, status = COALESCE(?, status), updated = now() WHERE id = ?",
            toJson(event.reason),
            event.status?.name,
            event.id.value
        )
    }

    fun on(event: PurchaseOrderCheckedOut) {
        val purchaseOrderId = event.id.value
        // UPSERT customer_order if present
        event.customerOrder?.let { co ->
            jdbcTemplate.update(
                """INSERT INTO customer_order (purchase_order_id, customer_order_id, status, steps)
                   VALUES (?, ?, ?, ?::jsonb)
                   ON CONFLICT (purchase_order_id)
                   DO UPDATE SET customer_order_id = ?, status = ?, steps = ?::jsonb""",
                purchaseOrderId, co.id, null, null,
                co.id, null, null
            )
        }
        // UPDATE purchase_order status to CHECKED_OUT
        jdbcTemplate.update(
            """UPDATE purchase_order
               SET status = 'CHECKED_OUT', channel_checkout = ?::jsonb, updated = now()
               WHERE id = ?""",
            toJson(event.channel),
            purchaseOrderId
        )
    }

    fun on(event: PurchaseOrderCouponCreated) {
        jdbcTemplate.update(
            """INSERT INTO purchase_order (id, status, type, customer, callback, channel_create,
               coupon_code, created, updated, version)
               VALUES (?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, now(), now(), 1)""",
            event.id.value,
            "OPENED",
            event.type?.name,
            event.customer?.id,
            toJson(event.callback),
            toJson(event.channel),
            event.coupon.code
        )
    }

    private fun toJson(value: Any?): String? {
        if (value == null) return null
        return objectMapper.writeValueAsString(value)
    }
}
