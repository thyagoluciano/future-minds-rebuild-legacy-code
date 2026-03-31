package br.com.zup.realwave.sales.manager.domain.exception

class PurchaseOrderNotFoundException(id: String) :
    RuntimeException("PurchaseOrder not found with id: $id")
