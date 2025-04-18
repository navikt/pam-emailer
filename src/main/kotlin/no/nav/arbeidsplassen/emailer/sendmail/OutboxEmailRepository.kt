package no.nav.arbeidsplassen.emailer.sendmail

import no.nav.arbeidsplassen.emailer.sendmail.EmailQuota.Companion.MAX_RETRIES_HIGH_PRIORITY_EMAIL
import no.nav.arbeidsplassen.emailer.sendmail.EmailQuota.Companion.MAX_RETRIES_NORMAL_PRIORITY_EMAIL
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

    private val countsRowMapper = RowMapper<EmailCounts> { rs, _ ->
        EmailCounts(
            sentLastHour = rs.getInt("sent_last_hour_count"),
            pending = rs.getInt("pending_count"),
            failed = rs.getInt("failed_count")
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
        """

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
        """

        val params = MapSqlParameterSource("id", id)

        return jdbcTemplate.query(sql, params, rowMapper).firstOrNull()
    }

    fun findPendingSortedByPriorityAndCreated(limit: Int, highPriorityOnly: Boolean): List<OutboxEmail> {
        var sql = """
            SELECT *
            FROM outbox_email
            WHERE status = :pending_status
         """

        if (highPriorityOnly) {
            sql += " AND priority = :high_priority"
        }

        sql += """
            ORDER BY priority DESC, created_at
            LIMIT :limit
        """

        val params = MapSqlParameterSource()
            .addValue("pending_status", Status.PENDING.toString())
            .addValue("limit", limit)
            .addValue("high_priority", Priority.HIGH.value)

        return jdbcTemplate.query(sql, params, rowMapper)
    }

    fun findFailedSortedByPriorityAndUpdated(limit: Int, highPriorityOnly: Boolean): List<OutboxEmail> {
        var sql = """
            SELECT *
            FROM outbox_email
            WHERE status = :failed_status
             AND ((priority = :normal_priority AND retries < :max_retries_normal_priority)
                    OR
                  (priority = :high_priority AND retries < :max_retries_high_priority))
        """

        if (highPriorityOnly) {
            sql += " AND priority = :high_priority"
        }

        sql += """
            ORDER BY priority DESC, updated_at
            LIMIT :limit
        """

        val params = MapSqlParameterSource()
            .addValue("failed_status", Status.FAILED.toString())
            .addValue("max_retries_normal_priority", MAX_RETRIES_NORMAL_PRIORITY_EMAIL)
            .addValue("max_retries_high_priority", MAX_RETRIES_HIGH_PRIORITY_EMAIL)
            .addValue("limit", limit)
            .addValue("high_priority", Priority.HIGH.value)
            .addValue("normal_priority", Priority.NORMAL.value)

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
        """

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
        """

        val params = MapSqlParameterSource()
            .addValue("one_hour_ago", oneHourAgo)
            .addValue("sent_status", Status.SENT.toString())

        return jdbcTemplate.queryForObject(sql, params, Int::class.java)!!
    }

    fun getEmailCounts(): EmailCounts {
        val sql = """
            SELECT
                (SELECT count(*) FROM outbox_email WHERE updated_at > :one_hour_ago AND status = :sent_status) as sent_last_hour_count,
                (SELECT count(*) FROM outbox_email WHERE status = :pending_status) as pending_count,
                (SELECT count(*) FROM outbox_email WHERE status = :failed_status) as failed_count
        """;

        val oneHourAgo = OffsetDateTime.now().minusHours(1)
        val params = MapSqlParameterSource()
            .addValue("one_hour_ago", oneHourAgo)
            .addValue("sent_status", Status.SENT.toString())
            .addValue("pending_status", Status.PENDING.toString())
            .addValue("failed_status", Status.FAILED.toString())

        return jdbcTemplate.queryForObject(sql, params, countsRowMapper)!!
    }

    fun deleteEmailsOlderThanAnHour() {
        val oneHourAgo = OffsetDateTime.now().minusHours(1)
        val sql = """
            DELETE 
            FROM outbox_email
            WHERE updated_at < :one_hour_ago
                AND status = :sent_status
        """

        val params = MapSqlParameterSource()
            .addValue("one_hour_ago", oneHourAgo)
            .addValue("sent_status", Status.SENT.toString())

        jdbcTemplate.update(sql, params)
    }

    fun deleteEmail(outboxEmail: OutboxEmail) {
        var sql = """
            DELETE 
            FROM outbox_email
            WHERE id = :id
        """

        val params = MapSqlParameterSource()
            .addValue("id", outboxEmail.id)

        jdbcTemplate.update(sql, params)
    }

}

data class EmailCounts(
    val sentLastHour: Int,
    val pending: Int,
    val failed: Int,
)
