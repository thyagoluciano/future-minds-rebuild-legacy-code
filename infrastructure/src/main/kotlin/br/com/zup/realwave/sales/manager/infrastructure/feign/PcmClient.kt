package br.com.zup.realwave.sales.manager.infrastructure.feign

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(name = "pcm", url = "\${pcm.url}", configuration = [FeignConfig::class])
interface PcmClient {

    @GetMapping("/compositions/{compositionId}")
    fun getComposition(@PathVariable compositionId: String): CompositionRepresentation

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class CompositionRepresentation(
        val id: String,
        val name: String?,
        val description: String?,
        val productType: ProductTypeRepresentation?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ProductTypeRepresentation(
        val id: String,
        val name: String?
    )
}
