package no.nav.arbeidsplassen.emailer.sendmail

import no.nav.arbeidsplassen.emailer.sendmail.EmailQuota.Companion.MAX_RETRIES_HIGH_PRIORITY_EMAIL
import no.nav.arbeidsplassen.emailer.sendmail.EmailQuota.Companion.MAX_RETRIES_NORMAL_PRIORITY_EMAIL
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
        if (status != Status.PENDING) {
            retries++
        }
        status = Status.FAILED
        updatedAt = OffsetDateTime.now()
    }

    fun tryNumber(): Int? {
        return when(status) {
            Status.PENDING -> 1
            Status.FAILED -> retries+1+1 // +1 for the initial try, +1 for the current try
            Status.SENT -> null
        }
    }

    fun maxNumberOfRetriesReached(): Boolean {
        return when(priority) {
            Priority.HIGH -> retries >= MAX_RETRIES_HIGH_PRIORITY_EMAIL
            Priority.NORMAL -> retries >= MAX_RETRIES_NORMAL_PRIORITY_EMAIL
        }
    }

    fun shouldBeDeleted(): Boolean {
        return maxNumberOfRetriesReached() && priority == Priority.NORMAL
    }
}

enum class Status {
    PENDING,
    SENT,
    FAILED
}
