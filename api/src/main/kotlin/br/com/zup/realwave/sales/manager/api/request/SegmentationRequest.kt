package br.com.zup.realwave.sales.manager.api.request

import jakarta.validation.constraints.NotBlank

data class SegmentationRequest(@field:NotBlank val queryString: String?)
