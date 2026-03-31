package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SalesForceTest {

    @Test
    fun `should create SalesForce with all null fields`() {
        val sf = SalesForce()
        assertNull(sf.agentId)
        assertNull(sf.supervisorId)
        assertNull(sf.channel)
    }

    @Test
    fun `should create SalesForce with all fields`() {
        val sf = SalesForce(agentId = "agent-1", supervisorId = "super-1", channel = "WEB")
        assertEquals("agent-1", sf.agentId)
        assertEquals("super-1", sf.supervisorId)
        assertEquals("WEB", sf.channel)
    }

    @Test
    fun `two SalesForces with same data should be equal`() {
        val s1 = SalesForce("a1", "s1", "WEB")
        val s2 = SalesForce("a1", "s1", "WEB")
        assertEquals(s1, s2)
    }
}
