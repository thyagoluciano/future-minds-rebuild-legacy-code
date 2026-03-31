package br.com.zup.realwave.sales.manager.infrastructure.feign

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(name = "coupon", url = "\${coupon.url}", configuration = [FeignConfig::class])
interface CouponClient {

    @GetMapping("/v1/coupons/code/{code}/customer/{customerId}/validation")
    fun validateCoupon(
        @PathVariable code: String,
        @PathVariable customerId: String
    ): CouponRepresentation

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class CouponRepresentation(
        val id: String?,
        val code: String?,
        val description: String?,
        val reward: RewardRepresentation?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class RewardRepresentation(
        val discounts: List<DiscountRepresentation>?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DiscountRepresentation(
        val type: String?,
        val amount: Long?,
        val currency: String?
    )
}
