package br.com.zup.realwave.sales.manager.infrastructure.kafka

import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OutboxPoller(
    private val jdbcTemplate: JdbcTemplate,
    private val kafkaTemplate: KafkaTemplate<String, String>
) {

    @Value("\${outbox.poll.interval-ms:5000}")
    private var intervalMs: Long = 5000

    companion object {
        const val TOPIC = "rw_sm_purchase_events"
        const val BATCH_SIZE = 100
    }

    @Scheduled(fixedDelayString = "\${outbox.poll.interval-ms:5000}")
    @Transactional
    fun pollAndPublish() {
        val rows = jdbcTemplate.queryForList(
            """
            SELECT id, aggregate_id, event_type, payload
            FROM outbox
            WHERE published = false
            ORDER BY created_at ASC
            LIMIT $BATCH_SIZE
            """.trimIndent()
        )

        for (row in rows) {
            val id = row["id"]
            val aggregateId = row["aggregate_id"] as String
            val payload = row["payload"] as String

            kafkaTemplate.send(TOPIC, aggregateId, payload)

            jdbcTemplate.update(
                "UPDATE outbox SET published = true, published_at = NOW() WHERE id = ?",
                id
            )
        }
    }
}
