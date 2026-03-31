package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OnBoardingSaleTest {

    @Test
    fun `should create OnBoardingSale with custom fields`() {
        val fields = mapOf("offer" to "offer-1")
        val obs = OnBoardingSale(customFields = fields)
        assertEquals(fields, obs.customFields)
    }

    @Test
    fun `two OnBoardingSales with same data should be equal`() {
        val o1 = OnBoardingSale(mapOf("k" to "v"))
        val o2 = OnBoardingSale(mapOf("k" to "v"))
        assertEquals(o1, o2)
    }
}
