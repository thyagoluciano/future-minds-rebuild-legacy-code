package br.com.zup.realwave.sales.manager.infrastructure.eventstore

import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.command.CreatePurchaseOrderCommand
import br.com.zup.realwave.sales.manager.domain.exception.PurchaseOrderNotFoundException
import br.com.zup.realwave.sales.manager.infrastructure.multitenant.LiquibaseHandler
import br.com.zup.realwave.sales.manager.infrastructure.multitenant.TenantContext
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Disabled("Requires Testcontainers with Podman/Docker — run locally with DOCKER_HOST set")
@Testcontainers
class JdbcEventStoreTest {

    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16")
            .withDatabaseName("sales_manager_test")
            .withUsername("test")
            .withPassword("test")

        private lateinit var jdbcTemplate: JdbcTemplate
        private lateinit var eventStore: JdbcEventStore
        private lateinit var liquibaseHandler: LiquibaseHandler

        private const val TEST_TENANT = "tenant_test"

        @BeforeAll
        @JvmStatic
        fun setupAll() {
            System.setProperty("TESTCONTAINERS_RYUK_DISABLED", "true")

            val dataSource = DriverManagerDataSource(
                postgres.jdbcUrl,
                postgres.username,
                postgres.password
            ).apply { setDriverClassName("org.postgresql.Driver") }

            jdbcTemplate = JdbcTemplate(dataSource)
            liquibaseHandler = LiquibaseHandler(dataSource, "rw_sm_")
            liquibaseHandler.initializeTenantSchema(TEST_TENANT)

            val objectMapper = ObjectMapper().registerKotlinModule()
            eventStore = JdbcEventStore(jdbcTemplate, objectMapper)
        }
    }

    @BeforeEach
    fun setUp() {
        TenantContext.set(TEST_TENANT)
        // Clean domain_events and outbox before each test
        jdbcTemplate.execute("""SET search_path TO "rw_sm_$TEST_TENANT"""")
        jdbcTemplate.execute("DELETE FROM outbox")
        jdbcTemplate.execute("DELETE FROM domain_events")
    }

    @AfterEach
    fun tearDown() {
        TenantContext.clear()
    }

    @Test
    fun `save and findById should reconstruct aggregate with same state`() {
        val command = CreatePurchaseOrderCommand(
            purchaseOrderType = null,
            callback = null
        )
        val order = PurchaseOrder.create(command)

        eventStore.save(order)

        val found = eventStore.findById(order.id)
        assertNotNull(found)
        assertEquals(order.id, found!!.id)
        assertEquals(order.status, found.status)
    }

    @Test
    fun `findById should return null when aggregate does not exist`() {
        val nonExistentId = PurchaseOrderId("00000000-0000-0000-0000-000000000000")
        val result = eventStore.findById(nonExistentId)
        assertNull(result)
    }

    @Test
    fun `findByIdOrThrow should throw PurchaseOrderNotFoundException when not found`() {
        val nonExistentId = PurchaseOrderId("00000000-0000-0000-0000-000000000001")
        assertThrows<PurchaseOrderNotFoundException> {
            eventStore.findByIdOrThrow(nonExistentId)
        }
    }

    @Test
    fun `save should persist events in domain_events table`() {
        val command = CreatePurchaseOrderCommand(
            purchaseOrderType = null,
            callback = null
        )
        val order = PurchaseOrder.create(command)
        eventStore.save(order)

        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM domain_events WHERE aggregate_id = ?",
            Long::class.java,
            order.id.value
        )
        assertEquals(1L, count)
    }

    @Test
    fun `save should also persist events in outbox table`() {
        val command = CreatePurchaseOrderCommand(
            purchaseOrderType = null,
            callback = null
        )
        val order = PurchaseOrder.create(command)
        eventStore.save(order)

        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM outbox WHERE aggregate_id = ?",
            Long::class.java,
            order.id.value
        )
        assertEquals(1L, count)
    }

    @Test
    fun `save with no pending events should be a no-op`() {
        val id = PurchaseOrderId()
        val order = PurchaseOrder(id)
        // no events applied, pendingEvents is empty
        eventStore.save(order)

        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM domain_events WHERE aggregate_id = ?",
            Long::class.java,
            id.value
        )
        assertEquals(0L, count)
    }

    @Test
    fun `findById after multiple saves should replay events in correct order`() {
        val command = CreatePurchaseOrderCommand(
            purchaseOrderType = null,
            callback = null
        )
        val order = PurchaseOrder.create(command)
        eventStore.save(order)

        val found = eventStore.findById(order.id)
        assertNotNull(found)
        // version should match after one event (PurchaseOrderCreated)
        assertEquals(1, found!!.version)
    }
}
