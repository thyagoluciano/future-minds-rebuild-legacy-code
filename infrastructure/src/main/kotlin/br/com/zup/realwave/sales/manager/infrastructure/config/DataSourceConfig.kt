package br.com.zup.realwave.sales.manager.infrastructure.config

import br.com.zup.realwave.sales.manager.infrastructure.multitenant.TenantRoutingDataSource
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

@Configuration
class DataSourceConfig {

    @Value("\${spring.datasource.url:\${DB_URL:jdbc:postgresql://localhost:5432/sales_manager}}")
    private lateinit var url: String

    @Value("\${spring.datasource.username:\${DB_USERNAME:postgres}}")
    private lateinit var username: String

    @Value("\${spring.datasource.password:\${DB_PASSWORD:postgres}}")
    private lateinit var password: String

    @Bean
    fun defaultDataSource(): DataSource {
        val config = HikariConfig().apply {
            jdbcUrl = url
            this.username = this@DataSourceConfig.username
            this.password = this@DataSourceConfig.password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            minimumIdle = 2
            poolName = "HikariPool-default"
        }
        return HikariDataSource(config)
    }

    @Bean
    @Primary
    fun tenantRoutingDataSource(): DataSource {
        val default = defaultDataSource()
        return TenantRoutingDataSource(
            tenantDataSourceFactory = { tenantId ->
                val config = HikariConfig().apply {
                    jdbcUrl = url
                    this.username = this@DataSourceConfig.username
                    this.password = this@DataSourceConfig.password
                    driverClassName = "org.postgresql.Driver"
                    maximumPoolSize = 5
                    minimumIdle = 1
                    poolName = "HikariPool-$tenantId"
                }
                HikariDataSource(config)
            },
            defaultDataSource = default
        )
    }
}
