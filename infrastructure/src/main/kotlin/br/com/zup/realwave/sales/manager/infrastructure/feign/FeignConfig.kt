package br.com.zup.realwave.sales.manager.infrastructure.feign

import br.com.zup.realwave.sales.manager.infrastructure.multitenant.TenantContext
import feign.RequestInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FeignConfig {

    companion object {
        const val TENANT_HEADER = "X-Realwave-Organization-Slug"
    }

    @Bean
    fun tenantFeignInterceptor(): RequestInterceptor = RequestInterceptor { template ->
        runCatching { TenantContext.get() }
            .onSuccess { tenantId -> template.header(TENANT_HEADER, tenantId) }
    }
}
