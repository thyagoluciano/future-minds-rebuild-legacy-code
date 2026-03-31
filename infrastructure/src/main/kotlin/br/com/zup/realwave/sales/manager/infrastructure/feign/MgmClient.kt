package br.com.zup.realwave.sales.manager.infrastructure.feign

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(name = "mgm", url = "\${mgm.url}", configuration = [FeignConfig::class])
interface MgmClient {

    @GetMapping("/v2/member/{memberGetMemberCode}/validate")
    fun validate(@PathVariable memberGetMemberCode: String)
}
