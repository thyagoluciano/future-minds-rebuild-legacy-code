package br.com.zup.realwave.sales.manager.infrastructure.multitenant

object TenantContext {
    private val tenant = ThreadLocal<String>()

    fun set(tenantId: String) {
        tenant.set(tenantId)
    }

    fun get(): String = tenant.get() ?: error("No tenant in context")

    fun clear() {
        tenant.remove()
    }
}
