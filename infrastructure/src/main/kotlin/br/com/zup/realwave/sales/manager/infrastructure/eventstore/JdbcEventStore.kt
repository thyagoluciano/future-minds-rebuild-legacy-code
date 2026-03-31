package br.com.zup.realwave.sales.manager.infrastructure.eventstore

import br.com.zup.realwave.sales.manager.domain.PurchaseOrder
import br.com.zup.realwave.sales.manager.domain.PurchaseOrderId
import br.com.zup.realwave.sales.manager.domain.exception.PurchaseOrderNotFoundException
import br.com.zup.realwave.sales.manager.domain.port.PurchaseOrderRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant

@Repository
class JdbcEventStore(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper
) : PurchaseOrderRepository {

    @Transactional
    override fun save(purchaseOrder: PurchaseOrder) {
        val pendingEvents = purchaseOrder.pendingEvents
        if (pendingEvents.isEmpty()) return

        var currentVersion = purchaseOrder.version - pendingEvents.size

        for (event in pendingEvents) {
            val eventType = event::class.simpleName!!
            val payload = objectMapper.writeValueAsString(event)
            val now = Timestamp.from(Instant.now())
            currentVersion++

            jdbcTemplate.update(
                """
                INSERT INTO domain_events (aggregate_id, aggregate_type, event_type, payload, version, created_at)
                VALUES (?, ?, ?, ?::jsonb, ?, ?)
                """.trimIndent(),
                purchaseOrder.id.value,
                "PurchaseOrder",
                eventType,
                payload,
                currentVersion,
                now
            )

            jdbcTemplate.update(
                """
                INSERT INTO outbox (aggregate_id, event_type, payload, created_at)
                VALUES (?, ?, ?::jsonb, ?)
                """.trimIndent(),
                purchaseOrder.id.value,
                eventType,
                payload,
                now
            )
        }

        purchaseOrder.clearPendingEvents()
    }

    override fun findById(id: PurchaseOrderId): PurchaseOrder? {
        val events = jdbcTemplate.query(
            """
            SELECT event_type, payload
            FROM domain_events
            WHERE aggregate_id = ?
            ORDER BY version ASC
            """.trimIndent(),
            { rs, _ ->
                val eventType = rs.getString("event_type")
                val payload = rs.getString("payload")
                val eventClass = EventTypeRegistry.resolve(eventType)
                objectMapper.readValue(payload, eventClass.java)
            },
            id.value
        )

        if (events.isEmpty()) return null

        val purchaseOrder = PurchaseOrder(id)
        for (event in events) {
            purchaseOrder.replayEvent(event)
        }
        return purchaseOrder
    }

    override fun findByIdOrThrow(id: PurchaseOrderId): PurchaseOrder {
        return findById(id) ?: throw PurchaseOrderNotFoundException(id.value)
    }
}
