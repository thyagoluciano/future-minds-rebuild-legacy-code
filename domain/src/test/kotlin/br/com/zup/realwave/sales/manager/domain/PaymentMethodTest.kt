package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PaymentMethodTest {

    @Test
    fun `should create PaymentMethod with valid type`() {
        val method = PaymentMethod(type = "CREDIT_CARD")
        assertEquals("CREDIT_CARD", method.type)
        assertNull(method.installments)
        assertNull(method.cardToken)
    }

    @Test
    fun `should create PaymentMethod with all fields`() {
        val price = Price.zero()
        val method = PaymentMethod(
            type = "CREDIT_CARD",
            installments = 3,
            installmentValue = price,
            totalValue = price,
            cardToken = "token-123"
        )
        assertEquals(3, method.installments)
        assertEquals("token-123", method.cardToken)
    }

    @Test
    fun `should throw exception when type is blank`() {
        assertThrows<IllegalArgumentException> { PaymentMethod(type = "") }
    }
}
