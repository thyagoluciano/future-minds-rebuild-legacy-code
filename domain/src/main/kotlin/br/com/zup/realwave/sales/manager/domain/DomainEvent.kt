package br.com.zup.realwave.sales.manager.domain

interface DomainEvent {
    fun apply(aggregate: AggregateRoot<*>)
}
