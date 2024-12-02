package no.nav.arbeidsplassen.emailer.sendmail

import java.time.OffsetDateTime
import java.util.*


data class OutboxEmail(
    val id: UUID,
    val emailId: String,
    var status: Status,
    var priority: Priority,
    val createdAt: OffsetDateTime,
    var updatedAt: OffsetDateTime,
    var retries: Int,
    var payload: String
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

    fun successfullySent() {
        status = Status.SENT
        updatedAt = OffsetDateTime.now()
        payload = ""
    }

    fun failedToSend() {
        status = Status.FAILED
        updatedAt = OffsetDateTime.now()
        retries++
    }
}

enum class Status {
    PENDING,
    SENT,
    FAILED
}