package br.com.zup.realwave.sales.manager.infrastructure.multitenant

import org.slf4j.LoggerFactory
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

class TenantRoutingDataSource(
    private val tenantDataSourceFactory: (String) -> DataSource,
    defaultDataSource: DataSource
) : AbstractRoutingDataSource() {

    private val log = LoggerFactory.getLogger(TenantRoutingDataSource::class.java)
    private val resolvedDataSources = ConcurrentHashMap<String, DataSource>()

    init {
        setDefaultTargetDataSource(defaultDataSource)
        setTargetDataSources(emptyMap<Any, Any>())
    }

    override fun determineCurrentLookupKey(): String = TenantContext.get()

    override fun determineTargetDataSource(): DataSource {
        val tenantId = determineCurrentLookupKey()
        return resolvedDataSources.getOrPut(tenantId) {
            log.info("Creating DataSource on-demand for tenant: {}", tenantId)
            tenantDataSourceFactory(tenantId)
        }
    }
}
