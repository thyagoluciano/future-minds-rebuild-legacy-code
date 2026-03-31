package br.com.zup.realwave.sales.manager.query.repository

import br.com.zup.realwave.sales.manager.api.response.CallbackResponse
import br.com.zup.realwave.sales.manager.api.response.ChannelResponse
import br.com.zup.realwave.sales.manager.api.response.CouponResponse
import br.com.zup.realwave.sales.manager.api.response.CustomerOrderStatusResponse
import br.com.zup.realwave.sales.manager.api.response.CustomerResponse
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
import br.com.zup.realwave.sales.manager.api.response.PricePerPeriodResponse
import br.com.zup.realwave.sales.manager.api.response.PriceResponse
import br.com.zup.realwave.sales.manager.api.response.PurchaseOrderResponse
import br.com.zup.realwave.sales.manager.api.response.PurchaseOrderStatusResponse
import br.com.zup.realwave.sales.manager.api.response.ReasonResponse
import br.com.zup.realwave.sales.manager.api.response.SalesForceResponse
import br.com.zup.realwave.sales.manager.api.response.StepResponse
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class PurchaseOrderQueryRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper
) {

    /**
     * Finds a purchase order by its ID, loading all related data (items, payment, freight,
     * customer_order, installation_attributes).
     */
    fun findById(id: String): PurchaseOrderResponse? {
        val sql = """
            SELECT po.id, po.status, po.type, po.customer, po.callback, po.channel_create,
                   po.channel_checkout, po.subscription_id, po.protocol, po.segmentation,
                   po.on_boarding_sale_offer_id, po.on_boarding_sale_custom_fields,
                   po.mgm_code, po.mgm_custom_fields,
                   po.sales_force_id, po.sales_force_name,
                   po.coupon_code, po.coupon_custom_fields,
                   po.payment_description, po.reason,
                   po.created, po.updated
            FROM purchase_order po
            WHERE po.id = ?
        """.trimIndent()

        val results = jdbcTemplate.query(sql, { rs, _ -> mapToPurchaseOrderResponse(rs, id) }, id)
        return results.firstOrNull()
    }

    /**
     * Finds a purchase order by protocol value.
     */
    fun findByProtocol(protocol: String): PurchaseOrderResponse? {
        val sql = """
            SELECT po.id, po.status, po.type, po.customer, po.callback, po.channel_create,
                   po.channel_checkout, po.subscription_id, po.protocol, po.segmentation,
                   po.on_boarding_sale_offer_id, po.on_boarding_sale_custom_fields,
                   po.mgm_code, po.mgm_custom_fields,
                   po.sales_force_id, po.sales_force_name,
                   po.coupon_code, po.coupon_custom_fields,
                   po.payment_description, po.reason,
                   po.created, po.updated
            FROM purchase_order po
            WHERE po.protocol = ?
        """.trimIndent()

        val results = jdbcTemplate.query(sql, { rs, _ ->
            val orderId = rs.getString("id")
            mapToPurchaseOrderResponse(rs, orderId)
        }, protocol)
        return results.firstOrNull()
    }

    /**
     * Finds all purchase orders for a given customer, with optional filters for status and date range.
     */
    fun findByCustomer(
        customerId: String,
        status: String? = null,
        start: String? = null,
        end: String? = null
    ): List<PurchaseOrderResponse> {
        val params = mutableListOf<Any>(customerId)
        val conditions = StringBuilder("""
            SELECT po.id, po.status, po.type, po.customer, po.callback, po.channel_create,
                   po.channel_checkout, po.subscription_id, po.protocol, po.segmentation,
                   po.on_boarding_sale_offer_id, po.on_boarding_sale_custom_fields,
                   po.mgm_code, po.mgm_custom_fields,
                   po.sales_force_id, po.sales_force_name,
                   po.coupon_code, po.coupon_custom_fields,
                   po.payment_description, po.reason,
                   po.created, po.updated
            FROM purchase_order po
            WHERE po.customer = ?
        """.trimIndent())

        if (status != null) {
            conditions.append(" AND po.status = ?")
            params.add(status)
        }
        if (start != null) {
            conditions.append(" AND po.created >= ?::timestamp")
            params.add(start)
        }
        if (end != null) {
            conditions.append(" AND po.created <= ?::timestamp")
            params.add(end)
        }

        return jdbcTemplate.query(conditions.toString(), { rs, _ ->
            val orderId = rs.getString("id")
            mapToPurchaseOrderResponse(rs, orderId)
        }, *params.toTypedArray())
    }

    /**
     * Returns the status and customer order data for a purchase order by ID.
     */
    fun getStatus(id: String): PurchaseOrderStatusResponse? {
        val sql = """
            SELECT po.id, po.status,
                   co.customer_order_id, co.status AS co_status, co.steps
            FROM purchase_order po
            LEFT JOIN customer_order co ON co.purchase_order_id = po.id
            WHERE po.id = ?
        """.trimIndent()

        val results = jdbcTemplate.query(sql, { rs, _ ->
            val poStatus = rs.getString("status")
            val coId = rs.getString("customer_order_id")
            val coStatus = rs.getString("co_status")
            val stepsJson = rs.getString("steps")

            val customerOrder = if (coId != null) {
                val steps = stepsJson?.let {
                    objectMapper.readValue(it, object : TypeReference<List<StepResponse>>() {})
                }
                CustomerOrderStatusResponse(
                    customerOrderId = coId,
                    status = coStatus,
                    steps = steps
                )
            } else null

            PurchaseOrderStatusResponse(
                status = poStatus,
                customerOrder = customerOrder
            )
        }, id)

        return results.firstOrNull()
    }

    // -------------------------------------------------------------------------
    // Private mapping helpers
    // -------------------------------------------------------------------------

    private fun mapToPurchaseOrderResponse(rs: ResultSet, purchaseOrderId: String): PurchaseOrderResponse {
        val customerId = rs.getString("customer")
        val callbackJson = rs.getString("callback")
        val channelCreateJson = rs.getString("channel_create")
        val channelCheckoutJson = rs.getString("channel_checkout")
        val segmentationJson = rs.getString("segmentation")
        val onBoardingSaleOfferId = rs.getString("on_boarding_sale_offer_id")
        val onBoardingSaleCustomFieldsJson = rs.getString("on_boarding_sale_custom_fields")
        val mgmCode = rs.getString("mgm_code")
        val mgmCustomFieldsJson = rs.getString("mgm_custom_fields")
        val salesForceId = rs.getString("sales_force_id")
        val salesForceName = rs.getString("sales_force_name")
        val couponCode = rs.getString("coupon_code")
        val couponCustomFieldsJson = rs.getString("coupon_custom_fields")
        val paymentDescription = rs.getString("payment_description")
        val reasonJson = rs.getString("reason")
        val protocol = rs.getString("protocol")

        val items = findItems(purchaseOrderId)
        val paymentMethods = findPaymentMethods(purchaseOrderId)
        val freight = findFreight(purchaseOrderId)
        val customerOrder = findCustomerOrder(purchaseOrderId)
        val installationAttributes = findInstallationAttributes(purchaseOrderId)

        val callback = callbackJson?.let {
            objectMapper.readValue(it, CallbackResponse::class.java)
        }

        val channelCreate = channelCreateJson?.let {
            objectMapper.readValue(it, ChannelResponse::class.java)
        }

        val channelCheckout = channelCheckoutJson?.let {
            objectMapper.readValue(it, ChannelResponse::class.java)
        }

        val segmentation = segmentationJson?.let {
            objectMapper.readTree(it)
        }

        val onBoardingSale = if (onBoardingSaleOfferId != null) {
            val fields = onBoardingSaleCustomFieldsJson?.let { objectMapper.readTree(it) }
            OnBoardingSaleResponse(id = onBoardingSaleOfferId, fields = fields)
        } else null

        val mgm = if (mgmCode != null) {
            val fields = mgmCustomFieldsJson?.let { objectMapper.readTree(it) }
            MgmResponse(code = mgmCode, fields = fields)
        } else null

        val salesForce = if (salesForceId != null || salesForceName != null) {
            SalesForceResponse(id = salesForceId, name = salesForceName)
        } else null

        val coupon = if (couponCode != null) {
            val fields = couponCustomFieldsJson?.let { objectMapper.readTree(it) }
            CouponResponse(id = couponCode, fields = fields)
        } else null

        val reason = reasonJson?.let {
            objectMapper.readValue(it, ReasonResponse::class.java)
        }

        val customer = customerId?.let { CustomerResponse(id = it) }

        val payment = PaymentResponse(
            methods = paymentMethods,
            description = paymentDescription?.let { DescriptionResponse(value = it) }
        )

        return PurchaseOrderResponse(
            id = rs.getString("id"),
            type = rs.getString("type"),
            protocol = protocol,
            subscriptionId = rs.getString("subscription_id"),
            segmentation = segmentation,
            mgm = mgm,
            salesForce = salesForce,
            onBoardingSale = onBoardingSale,
            customer = customer,
            coupon = coupon,
            totalPrice = null,
            discount = null,
            payment = payment,
            freight = freight,
            status = rs.getString("status"),
            items = items,
            installationAttributes = installationAttributes,
            channelCreate = channelCreate,
            channelCheckout = channelCheckout,
            callback = callback,
            reason = reason,
            createdAt = rs.getString("created"),
            updatedAt = rs.getString("updated")
        )
    }

    private fun findItems(purchaseOrderId: String): List<ItemResponse> {
        val sql = """
            SELECT id, catalog_offer_id, catalog_offer_type,
                   price_currency, price_amount, price_scale,
                   validity_period, validity_duration, validity_unlimited,
                   custom_fields, offer_items, prices_per_period, quantity
            FROM order_item
            WHERE purchase_order_id = ?
        """.trimIndent()

        return jdbcTemplate.query(sql, { rs, _ ->
            val customFieldsJson = rs.getString("custom_fields")
            val offerItemsJson = rs.getString("offer_items")
            val pricesPerPeriodJson = rs.getString("prices_per_period")

            val customFields: JsonNode? = customFieldsJson?.let { objectMapper.readTree(it) }

            val offerItems: List<OfferItemResponse> = offerItemsJson?.let {
                objectMapper.readValue(it, object : TypeReference<List<OfferItemResponse>>() {})
            } ?: emptyList()

            val pricesPerPeriod: List<PricePerPeriodResponse> = pricesPerPeriodJson?.let {
                objectMapper.readValue(it, object : TypeReference<List<PricePerPeriodResponse>>() {})
            } ?: emptyList()

            ItemResponse(
                id = rs.getString("id"),
                catalogOfferId = rs.getString("catalog_offer_id"),
                catalogOfferType = rs.getString("catalog_offer_type"),
                price = PriceResponse(
                    currency = rs.getString("price_currency"),
                    amount = rs.getInt("price_amount"),
                    scale = rs.getInt("price_scale")
                ),
                validity = OfferValidityResponse(
                    period = rs.getString("validity_period"),
                    duration = rs.getObject("validity_duration") as? Int,
                    unlimited = rs.getBoolean("validity_unlimited")
                ),
                customFields = customFields,
                offerItems = offerItems,
                pricesPerPeriod = pricesPerPeriod,
                quantity = rs.getObject("quantity") as? Int ?: 1
            )
        }, purchaseOrderId)
    }

    private fun findPaymentMethods(purchaseOrderId: String): List<PaymentMethodResponse> {
        val sql = """
            SELECT payment_method, payment_method_id,
                   price_currency, price_amount, price_scale,
                   installments, custom_fields
            FROM payment
            WHERE purchase_order_id = ?
            ORDER BY payment_order ASC
        """.trimIndent()

        return jdbcTemplate.query(sql, { rs, _ ->
            val customFieldsJson = rs.getString("custom_fields")
            val customFields: JsonNode? = customFieldsJson?.let { objectMapper.readTree(it) }

            val priceCurrency = rs.getString("price_currency")
            val price = if (priceCurrency != null) {
                PriceResponse(
                    currency = priceCurrency,
                    amount = rs.getInt("price_amount"),
                    scale = rs.getInt("price_scale")
                )
            } else null

            PaymentMethodResponse(
                method = rs.getString("payment_method"),
                methodId = rs.getString("payment_method_id"),
                price = price,
                customFields = customFields,
                securityCodeInformed = false,
                installments = rs.getObject("installments") as? Int
            )
        }, purchaseOrderId)
    }

    private fun findFreight(purchaseOrderId: String): FreightResponse? {
        val sql = """
            SELECT address, type, delivery_total_time,
                   price_currency, price_amount, price_scale
            FROM freight
            WHERE purchase_order_id = ?
        """.trimIndent()

        val results = jdbcTemplate.query(sql, { rs, _ ->
            val addressJson = rs.getString("address")
            val address = objectMapper.readValue(addressJson, FreightResponse.FreightAddressResponse::class.java)

            FreightResponse(
                address = address,
                price = FreightResponse.FreightPriceResponse(
                    currency = rs.getString("price_currency"),
                    amount = rs.getInt("price_amount"),
                    scale = rs.getInt("price_scale")
                ),
                type = rs.getString("type"),
                deliveryTotalTime = rs.getInt("delivery_total_time")
            )
        }, purchaseOrderId)

        return results.firstOrNull()
    }

    private fun findCustomerOrder(purchaseOrderId: String): CustomerOrderStatusResponse? {
        val sql = """
            SELECT customer_order_id, status, steps
            FROM customer_order
            WHERE purchase_order_id = ?
        """.trimIndent()

        val results = jdbcTemplate.query(sql, { rs, _ ->
            val stepsJson = rs.getString("steps")
            val steps: List<StepResponse>? = stepsJson?.let {
                objectMapper.readValue(it, object : TypeReference<List<StepResponse>>() {})
            }

            CustomerOrderStatusResponse(
                customerOrderId = rs.getString("customer_order_id"),
                status = rs.getString("status"),
                steps = steps
            )
        }, purchaseOrderId)

        return results.firstOrNull()
    }

    private fun findInstallationAttributes(purchaseOrderId: String): List<InstallationAttributesResponse> {
        val sql = """
            SELECT product_type_id, attributes
            FROM installation_attributes
            WHERE purchase_order_id = ?
        """.trimIndent()

        return jdbcTemplate.query(sql, { rs, _ ->
            val attributesJson = rs.getString("attributes")
            val attributes: Map<String, Any> = attributesJson?.let {
                objectMapper.readValue(it, object : TypeReference<Map<String, Any>>() {})
            } ?: emptyMap()

            InstallationAttributesResponse(
                productTypeId = rs.getString("product_type_id"),
                attributes = attributes
            )
        }, purchaseOrderId)
    }
}
