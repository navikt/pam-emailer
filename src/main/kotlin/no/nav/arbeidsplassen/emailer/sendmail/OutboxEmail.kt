package no.nav.arbeidsplassen.emailer.sendmail

import java.time.OffsetDateTime
import java.util.*


data class OutboxEmail(
    val id: UUID,
    val emailId: String,
    val status: Status,
    val priority: Priority,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val retries: Int,
    val payload: String
) {
    companion object {
        fun newOutboxEmail(emailId: String, priority: Priority, payload: String): OutboxEmail {
            val now = OffsetDateTime.now()
            return OutboxEmail(
                id = UUID.randomUUID(),
                emailId = emailId,
                status = Status.PENDING,
                priority = priority,
                createdAt = now,
                updatedAt = now,
                retries = 0,
                payload = payload
            )
        }
    }
}

enum class Status {
    PENDING,
    SENT,
    FAILED
}