package br.com.zup.realwave.sales.manager.domain.event

import br.com.zup.realwave.sales.manager.domain.AggregateRoot
import br.com.zup.realwave.sales.manager.domain.DomainEvent
import br.com.zup.realwave.sales.manager.domain.PurchaseOrder

abstract class PurchaseOrderApplicableEvent : DomainEvent {
    override fun apply(aggregate: AggregateRoot<*>) {
        applyTo(aggregate as PurchaseOrder)
    }

    abstract fun applyTo(purchaseOrder: PurchaseOrder)
}
