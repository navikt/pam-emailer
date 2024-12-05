package no.nav.arbeidsplassen.emailer.sendmail

import org.springframework.stereotype.Service
import kotlin.math.min

@Service
class EmailQuota(private val emailRepository: OutboxEmailRepository) {
    companion object {
        const val MAX_EMAILS_PER_HOUR = 2_850   // Actually 3000, but use a 5 % buffer to avoid hitting the limit
        const val HIGH_PRIORITY_EMAIL_BUFFER = 200

        // 10 every 10 seconds = max 3600 per hour
        const val PENDING_EMAIL_BATCH_SIZE = 10
        const val PENDING_EMAIL_CRON = "*/10 * * * * *"
        const val PENDING_EMAIL_LOCK_AT_LEAST_FOR = "PT8S"
        const val PENDING_EMAIL_LOCK_AT_MOST_FOR = "PT5M"

        // 100 every 5 minutes = max 1200 per hour
        const val RETRY_EMAIL_BATCH_SIZE = 100
        const val RETRY_EMAIL_CRON = "0 */5 * * * *"
        const val RETRY_EMAIL_LOCK_AT_LEAST_FOR = "PT4M"
        const val RETRY_EMAIL_LOCK_AT_MOST_FOR = "PT20M"
        const val MAX_RETRIES = 15
    }

    fun canSendEmailNow(outboxEmail: OutboxEmail): Boolean {
        val emailsSentInLastHour = emailRepository.countEmailsSentInLastHour()

        return when (outboxEmail.priority) {
            Priority.HIGH -> emailsSentInLastHour < MAX_EMAILS_PER_HOUR
            Priority.NORMAL -> emailsSentInLastHour < MAX_EMAILS_PER_HOUR - HIGH_PRIORITY_EMAIL_BUFFER
        }
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