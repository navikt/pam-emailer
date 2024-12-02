package no.nav.arbeidsplassen.emailer.sendmail

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.*


@Repository
class OutboxEmailRepository(val jdbcTemplate: NamedParameterJdbcTemplate) {

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

        return jdbcTemplate.queryForObject(sql, params) { rs, _ ->
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
    }

}

