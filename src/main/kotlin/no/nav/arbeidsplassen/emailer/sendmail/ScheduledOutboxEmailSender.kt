package no.nav.arbeidsplassen.emailer.sendmail

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.arbeidsplassen.emailer.sendmail.EmailQuota.Companion.PENDING_EMAIL_CRON
import no.nav.arbeidsplassen.emailer.sendmail.EmailQuota.Companion.PENDING_EMAIL_LOCK_AT_LEAST_FOR
import no.nav.arbeidsplassen.emailer.sendmail.EmailQuota.Companion.PENDING_EMAIL_LOCK_AT_MOST_FOR
import no.nav.arbeidsplassen.emailer.sendmail.EmailQuota.Companion.RETRY_EMAIL_CRON
import no.nav.arbeidsplassen.emailer.sendmail.EmailQuota.Companion.RETRY_EMAIL_LOCK_AT_LEAST_FOR
import no.nav.arbeidsplassen.emailer.sendmail.EmailQuota.Companion.RETRY_EMAIL_LOCK_AT_MOST_FOR
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ScheduledOutboxEmailSender(
    private val emailService: EmailService,
    private val emailQuota: EmailQuota,
    private val outboxEmailRepository: OutboxEmailRepository
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(ScheduledOutboxEmailSender::class.java)
    }

    @Scheduled(cron = PENDING_EMAIL_CRON)
    @SchedulerLock(
        name = "sendPendingEmails",
        lockAtLeastFor = PENDING_EMAIL_LOCK_AT_LEAST_FOR,
        lockAtMostFor = PENDING_EMAIL_LOCK_AT_MOST_FOR
    )
    fun sendPendingEmails() {
        val numberOfEmailsToSend = emailQuota.emailsToSend()

        if (numberOfEmailsToSend == 0) {
            LOG.debug("No quota left for sending emails")
            return
        }

        val emails = outboxEmailRepository.findPendingSortedByPriorityAndCreated(numberOfEmailsToSend)

        if (emails.isNotEmpty()) {
            LOG.info("Sending ${emails.size} pending emails (max batch size was $numberOfEmailsToSend)")

            emails.forEach {
                emailService.sendEmail(it)
            }
        }
    }

    @Scheduled(cron = RETRY_EMAIL_CRON)
    @SchedulerLock(name = "retryFailedEmails", lockAtLeastFor = RETRY_EMAIL_LOCK_AT_LEAST_FOR, lockAtMostFor = RETRY_EMAIL_LOCK_AT_MOST_FOR)
    fun retryFailedEmails() {
        val numberOfEmailsToSend = emailQuota.emailsToRetry()

        if (numberOfEmailsToSend == 0) {
            LOG.debug("No quota left for retrying failed emails")
            return
        }

        val emails = outboxEmailRepository.findFailedSortedByPriorityAndUpdated(numberOfEmailsToSend)

        if (emails.isNotEmpty()) {
            LOG.info("Retrying ${emails.size} failed emails (max batch size was $numberOfEmailsToSend)")

            emails.forEach {
                emailService.sendEmail(it)
            }
        }
    }
}