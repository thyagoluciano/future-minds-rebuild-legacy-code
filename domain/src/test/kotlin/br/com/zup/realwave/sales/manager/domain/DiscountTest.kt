package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class DiscountTest {

    @Test
    fun `should create Discount with valid fields`() {
        val discount = Discount(
            percentage = BigDecimal("10.0"),
            valueDiscount = Price.zero()
        )
        assertEquals(BigDecimal("10.0"), discount.percentage)
        assertEquals(Price.zero(), discount.valueDiscount)
        assertNull(discount.coupon)
    }

    @Test
    fun `should create Discount with coupon`() {
        val discount = Discount(
            percentage = BigDecimal("5.0"),
            valueDiscount = Price.zero(),
            coupon = "COUPON10"
        )
        assertEquals("COUPON10", discount.coupon)
    }

    @Test
    fun `two Discounts with same data should be equal`() {
        val d1 = Discount(BigDecimal.ZERO, Price.zero())
        val d2 = Discount(BigDecimal.ZERO, Price.zero())
        assertEquals(d1, d2)
    }

    @Test
    fun `should throw exception when percentage is negative`() {
        assertThrows<IllegalArgumentException> {
            Discount(percentage = BigDecimal("-1"), valueDiscount = Price.zero())
        }
    }
}
