package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PaymentTest {

    @Test
    fun `should create Payment with default empty methods`() {
        val payment = Payment()
        assertTrue(payment.methods.isEmpty())
        assertNull(payment.description)
    }

    @Test
    fun `couponPayment should return Payment with REWARD method`() {
        val payment = Payment.couponPayment()
        assertEquals(1, payment.methods.size)
        assertEquals("REWARD", payment.methods.first().type)
    }

    @Test
    fun `should create Payment with methods`() {
        val method = PaymentMethod(type = "CREDIT_CARD", installments = 3)
        val payment = Payment(methods = mutableListOf(method))
        assertEquals(1, payment.methods.size)
        assertEquals("CREDIT_CARD", payment.methods.first().type)
    }
}
