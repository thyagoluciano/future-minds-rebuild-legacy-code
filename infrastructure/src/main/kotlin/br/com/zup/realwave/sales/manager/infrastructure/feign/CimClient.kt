package br.com.zup.realwave.sales.manager.infrastructure.feign

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(name = "cim", url = "\${cim.url}", configuration = [FeignConfig::class])
interface CimClient {

    @GetMapping("/api/customers/{id}")
    fun findById(@PathVariable id: String): CustomerResponse

    @GetMapping("/api/customers/{customerId}/products/{productId}")
    fun findByCustomerIdAndProductId(
        @PathVariable customerId: String,
        @PathVariable productId: String
    ): CustomerResponse

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class CustomerResponse(
        val id: String,
        val status: String?
    )
}
