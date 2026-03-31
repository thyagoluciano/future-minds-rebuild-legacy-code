package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FreightTest {

    @Test
    fun `should create Freight with minimal fields`() {
        val freight = Freight(price = Price.zero())
        assertEquals(Price.zero(), freight.price)
        assertNull(freight.deliveryEstimateBusinessDays)
        assertNull(freight.type)
        assertNull(freight.address)
    }

    @Test
    fun `should create Freight with all fields`() {
        val address = Address(
            street = "Rua A",
            neighborhood = "Bairro B",
            city = "São Paulo",
            state = "SP",
            country = "BR",
            zipCode = "01310-100"
        )
        val freight = Freight(
            price = Price.zero(),
            deliveryEstimateBusinessDays = 5,
            latitude = "-23.5505",
            longitude = "-46.6333",
            type = "STANDARD",
            address = address
        )
        assertEquals(5, freight.deliveryEstimateBusinessDays)
        assertEquals("STANDARD", freight.type)
        assertNotNull(freight.address)
    }

    @Test
    fun `two Freights with same data should be equal`() {
        val f1 = Freight(price = Price.zero())
        val f2 = Freight(price = Price.zero())
        assertEquals(f1, f2)
    }
}
