package br.com.zup.realwave.sales.manager.command.controller

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
import br.com.zup.realwave.sales.manager.domain.CustomerOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.command.CheckoutCommand
import br.com.zup.realwave.sales.manager.domain.command.CreatePurchaseOrderCommand
import br.com.zup.realwave.sales.manager.domain.command.DeletePurchaseOrderCommand
import br.com.zup.realwave.sales.manager.domain.command.FindPurchaseOrderCommand
import br.com.zup.realwave.sales.manager.domain.command.ValidatePurchaseOrderCommand
import br.com.zup.realwave.sales.manager.domain.exception.PurchaseOrderNotFoundException
import br.com.zup.realwave.sales.manager.infrastructure.handler.GlobalExceptionHandler
import br.com.zup.realwave.sales.manager.infrastructure.multitenant.TenantContext
import br.com.zup.realwave.sales.manager.infrastructure.multitenant.TenantFilter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

private const val TENANT_HEADER = "X-Realwave-Organization-Slug"
private const val TENANT_VALUE = "test-tenant"

/**
 * Controller slice test for [PurchaseOrderCommandController].
 *
 * Uses [WebMvcTest] with a permissive [TestSecurityConfig] and includes [TenantFilter]
 * in the context so the slice exercises routing, request validation, and exception handling
 * via [GlobalExceptionHandler].
 *
 * Stubs are configured with mockito-kotlin's `whenever` / `any` which handles
 * Kotlin non-nullable parameter constraints correctly.
 */
@WebMvcTest(PurchaseOrderCommandController::class)
@Import(GlobalExceptionHandler::class, TestSecurityConfig::class, TenantFilter::class)
class PurchaseOrderCommandControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var commandHandler: PurchaseOrderCommandHandler

    @MockBean
    lateinit var couponCommandHandler: PurchaseOrderCouponCommandHandler

    @MockBean
    lateinit var segmentationCommandHandler: PurchaseOrderSegmentationCommandHandler

    @MockBean
    lateinit var onBoardingSaleCommandHandler: PurchaseOrderOnBoardingSaleCommandHandler

    @MockBean
    lateinit var mgmCommandHandler: PurchaseOrderMgmCommandHandler

    @MockBean
    lateinit var customerCommandHandler: PurchaseOrderCustomerCommandHandler

    @MockBean
    lateinit var itemCommandHandler: PurchaseOrderItemCommandHandler

    @MockBean
    lateinit var paymentCommandHandler: PurchaseOrderPaymentCommandHandler

    @MockBean
    lateinit var installationAttributesCommandHandler: PurchaseOrderInstallationAttributesCommandHandler

    @MockBean
    lateinit var customerOrderCommandHandler: PurchaseOrderCustomerOrderCommandHandler

    @MockBean
    lateinit var subscriptionCommandHandler: PurchaseOrderSubscriptionCommandHandler

    @MockBean
    lateinit var protocolCommandHandler: PurchaseOrderProtocolCommandHandler

    @MockBean
    lateinit var salesForceCommandHandler: PurchaseOrderSalesForceCommandHandler

    @MockBean
    lateinit var freightCommandHandler: PurchaseOrderFreightCommandHandler

    @AfterEach
    fun tearDown() {
        TenantContext.clear()
    }

    // ─── POST /purchase-orders ────────────────────────────────────────────────

    @Test
    fun `POST purchase-orders with no body and valid tenant returns 201 and id`() {
        val generatedId = PurchaseOrderId()
        whenever(commandHandler.handle(any<CreatePurchaseOrderCommand>())).doReturn(generatedId)

        mockMvc.perform(
            post("/purchase-orders")
                .header(TENANT_HEADER, TENANT_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(generatedId.value))
    }

    @Test
    fun `POST purchase-orders with valid type BUY returns 201`() {
        val generatedId = PurchaseOrderId()
        whenever(commandHandler.handle(any<CreatePurchaseOrderCommand>())).doReturn(generatedId)

        mockMvc.perform(
            post("/purchase-orders")
                .header(TENANT_HEADER, TENANT_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"type":"BUY"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
    }

    @Test
    fun `POST purchase-orders with invalid type returns 400`() {
        // Request-level validation rejects INVALID_TYPE before the handler is invoked
        mockMvc.perform(
            post("/purchase-orders")
                .header(TENANT_HEADER, TENANT_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"type":"INVALID_TYPE"}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST purchase-orders with valid CHANGE type returns 201`() {
        val generatedId = PurchaseOrderId()
        whenever(commandHandler.handle(any<CreatePurchaseOrderCommand>())).doReturn(generatedId)

        mockMvc.perform(
            post("/purchase-orders")
                .header(TENANT_HEADER, TENANT_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"type":"CHANGE"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isString)
    }

    // ─── GET /purchase-orders/{purchaseOrderId} ───────────────────────────────

    @Test
    fun `GET purchase-orders by id returns 404 when not found`() {
        val id = "non-existent-id"
        whenever(commandHandler.handle(any<FindPurchaseOrderCommand>()))
            .thenThrow(PurchaseOrderNotFoundException(id))

        mockMvc.perform(
            get("/purchase-orders/$id")
                .header(TENANT_HEADER, TENANT_VALUE)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errors[0].code").value("PURCHASE_ORDER_NOT_FOUND"))
    }

    @Test
    fun `GET purchase-orders by id returns 200 with OPENED status`() {
        val id = PurchaseOrderId()
        val order = PurchaseOrder.create(
            CreatePurchaseOrderCommand(id = id, purchaseOrderType = null, callback = null)
        )
        order.clearPendingEvents()
        whenever(commandHandler.handle(any<FindPurchaseOrderCommand>())).doReturn(order)

        mockMvc.perform(
            get("/purchase-orders/${id.value}")
                .header(TENANT_HEADER, TENANT_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id.value))
            .andExpect(jsonPath("$.status").value("OPENED"))
    }

    // ─── DELETE /purchase-orders/{purchaseOrderId} ────────────────────────────

    @Test
    fun `DELETE purchase-orders returns 200 when order exists`() {
        val id = PurchaseOrderId()
        doNothing().whenever(commandHandler).handle(any<DeletePurchaseOrderCommand>())

        mockMvc.perform(
            delete("/purchase-orders/${id.value}")
                .header(TENANT_HEADER, TENANT_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.purchaseOrderId").value(id.value))
    }

    @Test
    fun `DELETE purchase-orders returns 404 when order not found`() {
        val id = "non-existent"
        doThrow(PurchaseOrderNotFoundException(id)).whenever(commandHandler)
            .handle(any<DeletePurchaseOrderCommand>())

        mockMvc.perform(
            delete("/purchase-orders/$id")
                .header(TENANT_HEADER, TENANT_VALUE)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errors[0].code").value("PURCHASE_ORDER_NOT_FOUND"))
    }

    // ─── POST /purchase-orders/{purchaseOrderId}/checkout ────────────────────

    @Test
    fun `POST checkout with valid request returns 201 with customerOrder`() {
        val id = PurchaseOrderId()
        val customerOrderId = "customer-order-123"
        whenever(commandHandler.handle(any<CheckoutCommand>()))
            .doReturn(CustomerOrder(id = customerOrderId))

        mockMvc.perform(
            post("/purchase-orders/${id.value}/checkout")
                .header(TENANT_HEADER, TENANT_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(id.value))
            .andExpect(jsonPath("$.customerOrder.id").value(customerOrderId))
    }

    // ─── GET /purchase-orders/{purchaseOrderId}/validation ────────────────────

    @Test
    fun `GET validation with valid id returns 200`() {
        val id = PurchaseOrderId()
        doNothing().whenever(commandHandler).handle(any<ValidatePurchaseOrderCommand>())

        mockMvc.perform(
            get("/purchase-orders/${id.value}/validation")
                .header(TENANT_HEADER, TENANT_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.purchaseOrderId").value(id.value))
    }

    @Test
    fun `GET validation returns 404 when order not found`() {
        val id = "not-found"
        doThrow(PurchaseOrderNotFoundException(id)).whenever(commandHandler)
            .handle(any<ValidatePurchaseOrderCommand>())

        mockMvc.perform(
            get("/purchase-orders/$id/validation")
                .header(TENANT_HEADER, TENANT_VALUE)
        )
            .andExpect(status().isNotFound)
    }
}
