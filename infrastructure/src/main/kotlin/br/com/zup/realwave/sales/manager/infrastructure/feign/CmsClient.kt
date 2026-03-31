package br.com.zup.realwave.sales.manager.infrastructure.feign

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(name = "cms", url = "\${cms.url}", configuration = [FeignConfig::class])
interface CmsClient {

    @PostMapping("/offers/validate")
    fun validateOffers(@RequestBody request: OfferValidateRequest): OfferValidateResponse

    @GetMapping("/offers/{offerId}")
    fun getOffer(@PathVariable offerId: String): OfferRepresentation

    data class OfferValidateRequest(
        val items: List<OfferValidateItem>
    )

    data class OfferValidateItem(
        val id: String,
        val type: String,
        val items: List<OfferValidateItemDetail>
    )

    data class OfferValidateItemDetail(
        val id: String,
        val price: PriceRepresentation,
        val recurrent: Boolean
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class OfferValidateResponse(
        val errors: List<String> = emptyList()
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class OfferRepresentation(
        val id: String,
        val name: String?,
        val description: String?,
        val type: String?,
        val items: List<OfferItemRepresentation> = emptyList()
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class OfferItemRepresentation(
        val id: String,
        val price: PriceRepresentation?,
        val recurrent: Boolean = false,
        val productTypeId: String?,
        val productTypeName: String?,
        val compositionId: String?,
        val compositionName: String?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class PriceRepresentation(
        val amount: Long,
        val currency: String,
        val scale: Int = 2
    )
}
