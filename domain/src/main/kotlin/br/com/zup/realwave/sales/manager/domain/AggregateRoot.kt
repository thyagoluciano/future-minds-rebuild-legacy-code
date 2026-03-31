package br.com.zup.realwave.sales.manager.domain

abstract class AggregateRoot<ID> {
    abstract val id: ID
    private val _pendingEvents: MutableList<DomainEvent> = mutableListOf()
    val pendingEvents: List<DomainEvent> get() = _pendingEvents.toList()
    var version: Long = 0L
        protected set

    protected fun applyChange(event: DomainEvent) {
        replayEvent(event)
        _pendingEvents.add(event)
    }

    fun replayEvent(event: DomainEvent) {
        event.apply(this)
        version++
    }

    fun clearPendingEvents() = _pendingEvents.clear()
}
