package br.com.zup.realwave.sales.manager.infrastructure.multitenant

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Disabled("Requires Testcontainers with Podman/Docker — run locally with DOCKER_HOST set")
@Testcontainers
class LiquibaseHandlerTest {

    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16")
            .withDatabaseName("sales_manager_test")
            .withUsername("test")
            .withPassword("test")

        private lateinit var jdbcTemplate: JdbcTemplate
        private lateinit var liquibaseHandler: LiquibaseHandler

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
        }
    }

    @Test
    fun `initializeTenantSchema should create schema and apply all migrations`() {
        val tenantId = "liquibase_test"
        val schemaName = "rw_sm_$tenantId"

        liquibaseHandler.initializeTenantSchema(tenantId)

        val schemaExists = jdbcTemplate.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)",
            Boolean::class.java,
            schemaName
        )
        assertTrue(schemaExists == true, "Schema '$schemaName' should exist after initialization")
    }

    @Test
    fun `initializeTenantSchema should create domain_events table`() {
        val tenantId = "liquibase_events_test"
        val schemaName = "rw_sm_$tenantId"

        liquibaseHandler.initializeTenantSchema(tenantId)

        val tableExists = jdbcTemplate.queryForObject(
            """
            SELECT EXISTS(
                SELECT 1
                FROM information_schema.tables
                WHERE table_schema = ?
                AND table_name = 'domain_events'
            )
            """.trimIndent(),
            Boolean::class.java,
            schemaName
        )
        assertTrue(tableExists == true, "Table 'domain_events' should exist in schema '$schemaName'")
    }

    @Test
    fun `initializeTenantSchema should create outbox table`() {
        val tenantId = "liquibase_outbox_test"
        val schemaName = "rw_sm_$tenantId"

        liquibaseHandler.initializeTenantSchema(tenantId)

        val tableExists = jdbcTemplate.queryForObject(
            """
            SELECT EXISTS(
                SELECT 1
                FROM information_schema.tables
                WHERE table_schema = ?
                AND table_name = 'outbox'
            )
            """.trimIndent(),
            Boolean::class.java,
            schemaName
        )
        assertTrue(tableExists == true, "Table 'outbox' should exist in schema '$schemaName'")
    }

    @Test
    fun `initializeTenantSchema should be idempotent — running twice should not throw`() {
        val tenantId = "liquibase_idempotent_test"

        liquibaseHandler.initializeTenantSchema(tenantId)
        // Should not throw on second call
        liquibaseHandler.initializeTenantSchema(tenantId)

        val schemaExists = jdbcTemplate.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)",
            Boolean::class.java,
            "rw_sm_$tenantId"
        )
        assertTrue(schemaExists == true)
    }
}
