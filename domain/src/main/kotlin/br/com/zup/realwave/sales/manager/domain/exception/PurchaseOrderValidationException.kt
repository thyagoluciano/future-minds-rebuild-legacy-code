package br.com.zup.realwave.sales.manager.domain.exception

class PurchaseOrderValidationException(val errors: List<String>) :
    RuntimeException("PurchaseOrder validation failed: ${errors.joinToString(", ")}")
