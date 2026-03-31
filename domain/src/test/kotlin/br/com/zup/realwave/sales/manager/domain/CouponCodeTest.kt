package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CouponCodeTest {

    @Test
    fun `should create CouponCode with valid code`() {
        val coupon = CouponCode("DISCOUNT10")
        assertEquals("DISCOUNT10", coupon.code)
    }

    @Test
    fun `two CouponCodes with same code should be equal`() {
        val c1 = CouponCode("CODE")
        val c2 = CouponCode("CODE")
        assertEquals(c1, c2)
    }

    @Test
    fun `should throw exception when code is blank`() {
        assertThrows<IllegalArgumentException> { CouponCode("") }
        assertThrows<IllegalArgumentException> { CouponCode("   ") }
    }
}
