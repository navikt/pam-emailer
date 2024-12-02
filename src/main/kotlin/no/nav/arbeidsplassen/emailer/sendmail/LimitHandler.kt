package no.nav.arbeidsplassen.emailer.sendmail

import kotlin.math.min

class LimitHandler(private val emailRepository: OutboxEmailRepository) {
    companion object {
        const val MAX_EMAILS_PER_HOUR = 2_850   // Actually 3000, but use a 5 % buffer to avoid hitting the limit
        const val PENDING_EMAIL_BATCH_SIZE = 20
        const val RETRY_EMAIL_BATCH_SIZE = 20
        const val MAX_RETRIES_PER_EMAIL = 25
    }

    fun canSendEmailNow(): Boolean {
        val emailsSentInLastHour = emailRepository.countEmailsSentInLastHour()
        return emailsSentInLastHour < MAX_EMAILS_PER_HOUR
    }

    fun emailsToSend(): Int {
        val emailsSentInLastHour = emailRepository.countEmailsSentInLastHour()
        val emailsLeftToSendThisHour = MAX_EMAILS_PER_HOUR - emailsSentInLastHour

        return if (emailsLeftToSendThisHour <= 0) {
            0
        } else {
            min(emailsLeftToSendThisHour, PENDING_EMAIL_BATCH_SIZE)
        }
    }

    fun emailsToRetry(): Int {
        val emailsSentInLastHour = emailRepository.countEmailsSentInLastHour()
        val emailsLeftToSendThisHour = MAX_EMAILS_PER_HOUR - emailsSentInLastHour

        return if (emailsLeftToSendThisHour <= 0) {
            0
        } else {
            min(emailsLeftToSendThisHour, RETRY_EMAIL_BATCH_SIZE)
        }
    }
}