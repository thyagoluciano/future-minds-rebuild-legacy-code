package br.com.zup.realwave.sales.manager.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AggregateRootTest {

    private data class TestId(val value: String)

    private data class TestEvent(val message: String) : DomainEvent {
        override fun apply(aggregate: AggregateRoot<*>) {
            // no-op para teste
        }
    }

    private class TestAggregate(override val id: TestId) : AggregateRoot<TestId>() {
        val events = mutableListOf<TestEvent>()

        fun doSomething(msg: String) {
            applyChange(TestEvent(msg))
        }
    }

    @Test
    fun `applyChange should add event to pendingEvents and increment version`() {
        val aggregate = TestAggregate(TestId("test-1"))

        aggregate.doSomething("hello")

        assertEquals(1, aggregate.pendingEvents.size)
        assertEquals(1L, aggregate.version)
    }

    @Test
    fun `clearPendingEvents should empty pending events`() {
        val aggregate = TestAggregate(TestId("test-1"))
        aggregate.doSomething("hello")

        aggregate.clearPendingEvents()

        assertTrue(aggregate.pendingEvents.isEmpty())
    }

    @Test
    fun `replayEvent should increment version without adding to pendingEvents`() {
        val aggregate = TestAggregate(TestId("test-1"))

        aggregate.replayEvent(TestEvent("replay"))

        assertEquals(0, aggregate.pendingEvents.size)
        assertEquals(1L, aggregate.version)
    }
}
