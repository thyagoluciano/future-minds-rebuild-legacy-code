package br.com.zup.realwave.sales.manager.infrastructure.multitenant

import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.sql.Connection
import javax.sql.DataSource

@Component
class LiquibaseHandler(
    private val dataSource: DataSource,
    @Value("\${tenant.prefix:rw_sm_}") private val tenantPrefix: String
) {

    private val log = LoggerFactory.getLogger(LiquibaseHandler::class.java)

    fun initializeTenantSchema(tenantId: String) {
        val schemaName = "$tenantPrefix$tenantId"
        log.info("Initializing schema '{}' for tenant '{}'", schemaName, tenantId)

        dataSource.connection.use { connection ->
            createSchemaIfNotExists(connection, schemaName)
            runMigrations(connection, schemaName)
        }
    }

    private fun createSchemaIfNotExists(connection: Connection, schemaName: String) {
        connection.createStatement().use { stmt ->
            stmt.execute("CREATE SCHEMA IF NOT EXISTS \"$schemaName\"")
        }
        log.info("Schema '{}' is ready", schemaName)
    }

    private fun runMigrations(connection: Connection, schemaName: String) {
        connection.schema = schemaName
        val database = DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(JdbcConnection(connection))
        database.defaultSchemaName = schemaName

        val liquibase = Liquibase(
            "db/changelog/db.changelog-master.xml",
            ClassLoaderResourceAccessor(),
            database
        )

        liquibase.update("")
        log.info("Liquibase migrations applied to schema '{}'", schemaName)
    }
}
