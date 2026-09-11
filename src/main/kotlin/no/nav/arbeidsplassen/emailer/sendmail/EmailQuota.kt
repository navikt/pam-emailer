package no.nav.arbeidsplassen.emailer.sendmail

import org.springframework.stereotype.Service
import kotlin.math.min

@Service
class EmailQuota(private val emailRepository: OutboxEmailRepository) {
    companion object {
        const val MAX_EMAILS_PER_HOUR = 850
        const val HIGH_PRIORITY_EMAIL_BUFFER = 200

        // 2 every 8 seconds = max 900 per hour, capped at 850 by hourly quota
        const val PENDING_EMAIL_BATCH_SIZE = 2
        const val PENDING_EMAIL_CRON = "*/8 * * * * *"
        const val PENDING_EMAIL_LOCK_AT_LEAST_FOR = "PT8S"
        const val PENDING_EMAIL_LOCK_AT_MOST_FOR = "PT5M"

        // 2 every 8 seconds = max 900 per hour, capped at 850 by hourly quota
        const val RETRY_EMAIL_BATCH_SIZE = 2
        const val RETRY_EMAIL_CRON = "*/8 * * * * *"
        const val RETRY_EMAIL_LOCK_AT_LEAST_FOR = "PT4M"
        const val RETRY_EMAIL_LOCK_AT_MOST_FOR = "PT20M"

        const val MAX_RETRIES_NORMAL_PRIORITY_EMAIL = 1
        const val MAX_RETRIES_HIGH_PRIORITY_EMAIL = 50
    }

    fun canSendEmailNow(outboxEmail: OutboxEmail): Boolean {
        val emailsSentInLastHour = emailRepository.countEmailsSentInLastHour()

        return when (outboxEmail.priority) {
            Priority.HIGH -> emailsSentInLastHour < MAX_EMAILS_PER_HOUR
            Priority.NORMAL -> emailsSentInLastHour < MAX_EMAILS_PER_HOUR - HIGH_PRIORITY_EMAIL_BUFFER
        }
    }

    fun getPendingEmailsMaxBatchSize(): BatchSize {
        val emailsSentInLastHour = emailRepository.countEmailsSentInLastHour()
        val emailsLeftToSendThisHour = MAX_EMAILS_PER_HOUR - emailsSentInLastHour

        return if (emailsLeftToSendThisHour <= 0) {
            BatchSize(0)
        } else {
            val batchSize = min(emailsLeftToSendThisHour, PENDING_EMAIL_BATCH_SIZE)
            val highPriorityOnly = emailsLeftToSendThisHour <= HIGH_PRIORITY_EMAIL_BUFFER

            return BatchSize(batchSize, highPriorityOnly)
        }
    }

    fun getRetryFailedEmailsMaxBatchSize(): BatchSize {
        val emailsSentInLastHour = emailRepository.countEmailsSentInLastHour()
        val emailsLeftToSendThisHour = MAX_EMAILS_PER_HOUR - emailsSentInLastHour

        return if (emailsLeftToSendThisHour <= 0) {
            BatchSize(0)
        } else {
            val batchSize = min(emailsLeftToSendThisHour, RETRY_EMAIL_BATCH_SIZE)
            val highPriorityOnly = emailsLeftToSendThisHour <= HIGH_PRIORITY_EMAIL_BUFFER

            return BatchSize(batchSize, highPriorityOnly)
        }
    }

    data class BatchSize(
        val numberOfEmails: Int,
        val highPriorityOnly: Boolean = false
    )
}