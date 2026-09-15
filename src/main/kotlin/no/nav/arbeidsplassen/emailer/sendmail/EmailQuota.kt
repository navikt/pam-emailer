package no.nav.arbeidsplassen.emailer.sendmail

import org.springframework.stereotype.Service
import kotlin.math.min

@Service
class EmailQuota(private val emailRepository: OutboxEmailRepository) {
    companion object {
        // Limit is actually 4000 per hour but we've had issues at 2850
        const val MAX_EMAILS_PER_HOUR = 1900
        const val HIGH_PRIORITY_EMAIL_BUFFER = 200

        // 5 every 10 seconds = 1800 per hour
        const val PENDING_EMAIL_BATCH_SIZE = 5
        const val PENDING_EMAIL_CRON = "*/10 * * * * *"
        const val PENDING_EMAIL_LOCK_AT_LEAST_FOR = "PT8S"
        const val PENDING_EMAIL_LOCK_AT_MOST_FOR = "PT5M"

        // 80 every 5 minutes = 960 per hour
        const val RETRY_EMAIL_BATCH_SIZE = 80
        const val RETRY_EMAIL_CRON = "0 */5 * * * *"
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