package br.com.zup.realwave.sales.manager.query.controller

import br.com.zup.realwave.sales.manager.api.response.PaymentResponse
import br.com.zup.realwave.sales.manager.api.response.PurchaseOrderResponse
import br.com.zup.realwave.sales.manager.api.response.PurchaseOrderStatusResponse
import br.com.zup.realwave.sales.manager.infrastructure.handler.GlobalExceptionHandler
import br.com.zup.realwave.sales.manager.query.repository.PurchaseOrderQueryRepository
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(PurchaseOrderQueryController::class)
@Import(GlobalExceptionHandler::class)
class PurchaseOrderQueryControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var repository: PurchaseOrderQueryRepository

    private fun buildPurchaseOrderResponse(id: String, status: String = "OPENED"): PurchaseOrderResponse =
        PurchaseOrderResponse(
            id = id,
            type = "ACQUISITION",
            protocol = "PROT-001",
            subscriptionId = null,
            segmentation = null,
            mgm = null,
            salesForce = null,
            onBoardingSale = null,
            customer = null,
            coupon = null,
            totalPrice = null,
            discount = null,
            payment = PaymentResponse(methods = emptyList(), description = null),
            freight = null,
            status = status,
            items = emptyList(),
            installationAttributes = emptyList(),
            channelCreate = null,
            channelCheckout = null,
            callback = null,
            reason = null,
            createdAt = "2024-01-01T00:00:00",
            updatedAt = "2024-01-01T00:00:00"
        )

    @Test
    @WithMockUser
    fun `GET purchase-orders by id returns 200 when found`() {
        val id = "123"
        every { repository.findById(id) } returns buildPurchaseOrderResponse(id)

        mockMvc.perform(
            get("/purchase-orders/$id")
                .header("X-Realwave-Organization-Slug", "tenant1")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.status").value("OPENED"))
    }

    @Test
    @WithMockUser
    fun `GET purchase-orders by id returns 404 when not found`() {
        val id = "not-found"
        every { repository.findById(id) } returns null

        mockMvc.perform(
            get("/purchase-orders/$id")
                .header("X-Realwave-Organization-Slug", "tenant1")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errors[0].code").value("PURCHASE_ORDER_NOT_FOUND"))
    }

    @Test
    @WithMockUser
    fun `GET purchase-orders status returns 200 when found`() {
        val id = "123"
        val statusResponse = PurchaseOrderStatusResponse(status = "OPENED", customerOrder = null)
        every { repository.getStatus(id) } returns statusResponse

        mockMvc.perform(
            get("/purchase-orders/$id/status")
                .header("X-Realwave-Organization-Slug", "tenant1")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("OPENED"))
    }

    @Test
    @WithMockUser
    fun `GET purchase-orders status returns 404 when not found`() {
        val id = "not-found"
        every { repository.getStatus(id) } returns null

        mockMvc.perform(
            get("/purchase-orders/$id/status")
                .header("X-Realwave-Organization-Slug", "tenant1")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser
    fun `GET purchase-orders by customer returns list`() {
        val customerId = "customer-1"
        val orders = listOf(
            buildPurchaseOrderResponse("order-1"),
            buildPurchaseOrderResponse("order-2")
        )
        every { repository.findByCustomer(customerId, null, null, null) } returns orders

        mockMvc.perform(
            get("/purchase-orders")
                .param("customerId", customerId)
                .header("X-Realwave-Organization-Slug", "tenant1")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value("order-1"))
            .andExpect(jsonPath("$[1].id").value("order-2"))
    }

    @Test
    @WithMockUser
    fun `GET purchase-orders by customer with status filter returns filtered list`() {
        val customerId = "customer-1"
        val status = "OPENED"
        val orders = listOf(buildPurchaseOrderResponse("order-1", status))
        every { repository.findByCustomer(customerId, status, null, null) } returns orders

        mockMvc.perform(
            get("/purchase-orders")
                .param("customerId", customerId)
                .param("status", status)
                .header("X-Realwave-Organization-Slug", "tenant1")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value("order-1"))
            .andExpect(jsonPath("$[0].status").value(status))
    }

    @Test
    @WithMockUser
    fun `GET purchase-orders by protocol returns 200 when found`() {
        val protocol = "PROT-001"
        val order = buildPurchaseOrderResponse("123")
        every { repository.findByProtocol(protocol) } returns order

        mockMvc.perform(
            get("/purchase-orders/$protocol/protocol")
                .header("X-Realwave-Organization-Slug", "tenant1")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("123"))
            .andExpect(jsonPath("$.protocol").value("PROT-001"))
    }

    @Test
    @WithMockUser
    fun `GET purchase-orders by protocol returns 404 when not found`() {
        val protocol = "PROT-NOT-FOUND"
        every { repository.findByProtocol(protocol) } returns null

        mockMvc.perform(
            get("/purchase-orders/$protocol/protocol")
                .header("X-Realwave-Organization-Slug", "tenant1")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errors[0].code").value("PURCHASE_ORDER_NOT_FOUND"))
    }
}
