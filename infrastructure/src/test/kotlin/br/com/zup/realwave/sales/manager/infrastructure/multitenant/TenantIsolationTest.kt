package br.com.zup.realwave.sales.manager.infrastructure.multitenant

import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.command.CreatePurchaseOrderCommand
import br.com.zup.realwave.sales.manager.infrastructure.eventstore.JdbcEventStore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Tests that per-tenant schema isolation is enforced:
 * data saved in tenant A's schema must not be visible in tenant B's schema.
 */
@Disabled("Requires Testcontainers with Podman/Docker — run locally with DOCKER_HOST set")
@Testcontainers
class TenantIsolationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16")
            .withDatabaseName("sales_manager_test")
            .withUsername("test")
            .withPassword("test")

        private lateinit var jdbcTemplate: JdbcTemplate
        private lateinit var tenantAStore: JdbcEventStore
        private lateinit var tenantBStore: JdbcEventStore
        private lateinit var liquibaseHandler: LiquibaseHandler

        private const val TENANT_A = "tenant_isolation_a"
        private const val TENANT_B = "tenant_isolation_b"

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

            liquibaseHandler.initializeTenantSchema(TENANT_A)
            liquibaseHandler.initializeTenantSchema(TENANT_B)

            val objectMapper = ObjectMapper().registerKotlinModule()
            tenantAStore = JdbcEventStore(jdbcTemplate, objectMapper)
            tenantBStore = JdbcEventStore(jdbcTemplate, objectMapper)
        }
    }

    @AfterEach
    fun tearDown() {
        TenantContext.clear()
    }

    @Test
    fun `aggregate saved in tenant A is not visible in tenant B`() {
        val command = CreatePurchaseOrderCommand(purchaseOrderType = null, callback = null)
        val order = PurchaseOrder.create(command)
        val orderId = order.id

        // Save in tenant A
        TenantContext.set(TENANT_A)
        jdbcTemplate.execute("""SET search_path TO "rw_sm_$TENANT_A"""")
        tenantAStore.save(order)

        // Read from tenant B — should not find the same aggregate
        TenantContext.set(TENANT_B)
        jdbcTemplate.execute("""SET search_path TO "rw_sm_$TENANT_B"""")
        val foundInB = tenantBStore.findById(orderId)

        assertNull(foundInB, "Aggregate from tenant A must not be visible in tenant B")
    }

    @Test
    fun `aggregate saved in tenant A is findable within tenant A`() {
        val command = CreatePurchaseOrderCommand(purchaseOrderType = null, callback = null)
        val order = PurchaseOrder.create(command)
        val orderId = order.id

        TenantContext.set(TENANT_A)
        jdbcTemplate.execute("""SET search_path TO "rw_sm_$TENANT_A"""")
        tenantAStore.save(order)

        val foundInA = tenantAStore.findById(orderId)
        assertNotNull(foundInA)
        assertEquals(orderId, foundInA!!.id)
    }

    @Test
    fun `different tenants can have aggregates with the same ID independently`() {
        val sharedId = PurchaseOrderId("shared-tenant-id-test")

        // Tenant A: create an order with the shared ID explicitly
        val commandA = CreatePurchaseOrderCommand(id = sharedId, purchaseOrderType = null, callback = null)
        val orderA = PurchaseOrder.create(commandA)

        TenantContext.set(TENANT_A)
        jdbcTemplate.execute("""SET search_path TO "rw_sm_$TENANT_A"""")
        tenantAStore.save(orderA)

        // Tenant B: should have nothing under that same ID
        TenantContext.set(TENANT_B)
        jdbcTemplate.execute("""SET search_path TO "rw_sm_$TENANT_B"""")
        val foundInB = tenantBStore.findById(sharedId)

        assertNull(foundInB, "Tenant B must not see data that was saved under tenant A")
    }

    @Test
    fun `event counts are isolated per tenant`() {
        val commandA = CreatePurchaseOrderCommand(purchaseOrderType = null, callback = null)
        val orderA = PurchaseOrder.create(commandA)

        TenantContext.set(TENANT_A)
        jdbcTemplate.execute("""SET search_path TO "rw_sm_$TENANT_A"""")
        tenantAStore.save(orderA)

        TenantContext.set(TENANT_B)
        jdbcTemplate.execute("""SET search_path TO "rw_sm_$TENANT_B"""")
        val tenantBCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM domain_events WHERE aggregate_id = ?",
            Long::class.java,
            orderA.id.value
        )
        assertEquals(0L, tenantBCount, "Tenant B should have no events from tenant A's aggregate")
    }
}
