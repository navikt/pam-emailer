package no.nav.arbeidsplassen.emailer.sendmail

import no.nav.arbeidsplassen.emailer.sendmail.EmailQuota.Companion.MAX_RETRIES
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.*


@Repository
class OutboxEmailRepository(val jdbcTemplate: NamedParameterJdbcTemplate) {
    private val rowMapper = RowMapper<OutboxEmail> { rs, _ ->
        OutboxEmail(
            id = rs.getObject("id", UUID::class.java),
            emailId = rs.getString("email_id"),
            status = Status.valueOf(rs.getString("status")),
            priority = Priority.fromValue(rs.getInt("priority")),
            createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
            updatedAt = rs.getObject("updated_at", OffsetDateTime::class.java),
            retries = rs.getInt("retries"),
            payload = rs.getString("payload")
        )
    }

    fun create(outboxEmail: OutboxEmail) {
        val sql = """
            INSERT INTO outbox_email (
                id,
                email_id,
                status,
                priority,
                created_at,
                updated_at,
                retries,
                payload
            )
            VALUES (
                :id,
                :email_id,
                :status,
                :priority,
                :created_at,
                :updated_at,
                :retries,
                :payload
            )
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("id", outboxEmail.id)
            .addValue("email_id", outboxEmail.emailId)
            .addValue("status", outboxEmail.status.toString())
            .addValue("priority", outboxEmail.priority.value)
            .addValue("created_at", outboxEmail.createdAt)
            .addValue("updated_at", outboxEmail.updatedAt)
            .addValue("retries", outboxEmail.retries)
            .addValue("payload", outboxEmail.payload)

        jdbcTemplate.update(sql, params)
    }

    fun findById(id: UUID): OutboxEmail? {
        val sql = """
            SELECT *
            FROM outbox_email
            WHERE id = :id
        """.trimIndent()
        val params = MapSqlParameterSource("id", id)

        return jdbcTemplate.queryForObject(sql, params, rowMapper)
    }

    fun findPendingSortedByPriorityAndCreated(limit: Int): List<OutboxEmail> {
        val sql = """
            SELECT *
            FROM outbox_email
            WHERE status = :pending_status
            ORDER BY priority DESC, created_at
            LIMIT :limit
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("pending_status", Status.PENDING.toString())
            .addValue("limit", limit)

        return jdbcTemplate.query(sql, params, rowMapper)
    }

    fun findFailedSortedByPriorityAndUpdated(limit: Int): List<OutboxEmail> {
        val sql = """
            SELECT *
            FROM outbox_email
            WHERE status = :failed_status
             AND retries < :max_retries
            ORDER BY priority DESC, updated_at
            LIMIT :limit
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("failed_status", Status.FAILED.toString())
            .addValue("max_retries", MAX_RETRIES)
            .addValue("limit", limit)

        return jdbcTemplate.query(sql, params, rowMapper)
    }

    fun update(outboxEmail: OutboxEmail) {
        val sql = """
            UPDATE outbox_email
            SET
                email_id = :email_id,
                status = :status,
                priority = :priority,
                created_at = :created_at,
                updated_at = :updated_at,
                retries = :retries,
                payload = :payload
            WHERE id = :id
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", outboxEmail.id)
            .addValue("email_id", outboxEmail.emailId)
            .addValue("status", outboxEmail.status.toString())
            .addValue("priority", outboxEmail.priority.value)
            .addValue("created_at", outboxEmail.createdAt)
            .addValue("updated_at", outboxEmail.updatedAt)
            .addValue("retries", outboxEmail.retries)
            .addValue("payload", outboxEmail.payload)

        jdbcTemplate.update(sql, params)
    }

    fun countEmailsSentInLastHour(): Int {
        val oneHourAgo = OffsetDateTime.now().minusHours(1)
        val sql = """
            SELECT count(*)
            FROM outbox_email
            WHERE updated_at > :one_hour_ago
                AND status = :sent_status
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("one_hour_ago", oneHourAgo)
            .addValue("sent_status", Status.SENT.toString())

        return jdbcTemplate.queryForObject(sql, params, Int::class.java)!!
    }

}

