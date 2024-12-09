package no.nav.arbeidsplassen.emailer.sendmail

import io.mockk.every
import io.mockk.mockk
import no.nav.arbeidsplassen.emailer.sendmail.EmailQuota.Companion.HIGH_PRIORITY_EMAIL_BUFFER
import no.nav.arbeidsplassen.emailer.sendmail.EmailQuota.Companion.MAX_EMAILS_PER_HOUR
import no.nav.arbeidsplassen.emailer.sendmail.EmailQuota.Companion.PENDING_EMAIL_BATCH_SIZE
import no.nav.arbeidsplassen.emailer.sendmail.EmailQuota.Companion.RETRY_EMAIL_BATCH_SIZE
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EmailQuotaTest {

    private val emailRepository = mockk<OutboxEmailRepository>()
    private val emailQuota: EmailQuota = EmailQuota(emailRepository)

    @Test
    fun `Can send email now when not hitting limit`() {
        every { emailRepository.countEmailsSentInLastHour() } returns 0

        val normalPriorityEmail = OutboxEmail.newOutboxEmail("email-id", Priority.NORMAL, "payload")
        val highPriorityEmail = OutboxEmail.newOutboxEmail("email-id", Priority.HIGH, "payload")

        assertTrue(emailQuota.canSendEmailNow(normalPriorityEmail))
        assertTrue(emailQuota.canSendEmailNow(highPriorityEmail))
    }

    @Test
    fun `Can not send email now when limit is hit`() {
        every { emailRepository.countEmailsSentInLastHour() } returns MAX_EMAILS_PER_HOUR + 1

        val normalPriorityEmail = OutboxEmail.newOutboxEmail("email-id", Priority.NORMAL, "payload")
        val highPriorityEmail = OutboxEmail.newOutboxEmail("email-id", Priority.HIGH, "payload")

        assertFalse(emailQuota.canSendEmailNow(normalPriorityEmail))
        assertFalse(emailQuota.canSendEmailNow(highPriorityEmail))
    }

    @Test
    fun `Can not send NORMAL priority email now when NORMAL limit is hit`() {
        every { emailRepository.countEmailsSentInLastHour() } returns MAX_EMAILS_PER_HOUR - HIGH_PRIORITY_EMAIL_BUFFER + 1

        val normalPriorityEmail = OutboxEmail.newOutboxEmail("email-id", Priority.NORMAL, "payload")

        assertFalse(emailQuota.canSendEmailNow(normalPriorityEmail))
    }

    @Test
    fun `Can send HIGH priority email now when NORMAL limit is hit`() {
        every { emailRepository.countEmailsSentInLastHour() } returns MAX_EMAILS_PER_HOUR - HIGH_PRIORITY_EMAIL_BUFFER + 1

        val highPriorityEmail = OutboxEmail.newOutboxEmail("email-id", Priority.HIGH, "payload")

        assertTrue(emailQuota.canSendEmailNow(highPriorityEmail))
    }

    @Test
    fun `Will return pending email batch size, when there are more emails than batch size left to send`() {
        every { emailRepository.countEmailsSentInLastHour() } returns MAX_EMAILS_PER_HOUR - PENDING_EMAIL_BATCH_SIZE - 10

        assertEquals(PENDING_EMAIL_BATCH_SIZE, emailQuota.getPendingEmailsMaxBatchSize().numberOfEmails)
    }

    @Test
    fun `Will return 0 emails to send when there are no more emails to send`() {
        every { emailRepository.countEmailsSentInLastHour() } returns MAX_EMAILS_PER_HOUR

        assertEquals(0, emailQuota.getPendingEmailsMaxBatchSize().numberOfEmails)
    }

    @Test
    fun `Will return count of emails left to send, when there are less than batch size emails left to send`() {
        every { emailRepository.countEmailsSentInLastHour() } returns MAX_EMAILS_PER_HOUR - PENDING_EMAIL_BATCH_SIZE + 5

        assertEquals(PENDING_EMAIL_BATCH_SIZE - 5, emailQuota.getPendingEmailsMaxBatchSize().numberOfEmails)
    }

    @Test
    fun `Will not set high priority flag for pending emails if normal limit is not hit`() {
        every { emailRepository.countEmailsSentInLastHour() } returns MAX_EMAILS_PER_HOUR - HIGH_PRIORITY_EMAIL_BUFFER - 1

        val batchSize = emailQuota.getPendingEmailsMaxBatchSize()

        assertFalse(batchSize.highPriorityOnly)
    }

    @Test
    fun `Will set high priority flag for pending emails when normal limit is hit, but high priority buffer limit is not hit`() {
        every { emailRepository.countEmailsSentInLastHour() } returns MAX_EMAILS_PER_HOUR - HIGH_PRIORITY_EMAIL_BUFFER + 1

        val batchSize = emailQuota.getPendingEmailsMaxBatchSize()

        assertTrue(batchSize.highPriorityOnly)
    }

    @Test
    fun `Will return retry email batch size, when there are more emails than batch size left to send`() {
        every { emailRepository.countEmailsSentInLastHour() } returns MAX_EMAILS_PER_HOUR - RETRY_EMAIL_BATCH_SIZE - 10

        assertEquals(RETRY_EMAIL_BATCH_SIZE, emailQuota.getRetryFailedEmailsMaxBatchSize().numberOfEmails)
    }

    @Test
    fun `Will return 0 emails to send when there are no more emails to retry`() {
        every { emailRepository.countEmailsSentInLastHour() } returns MAX_EMAILS_PER_HOUR

        assertEquals(0, emailQuota.getRetryFailedEmailsMaxBatchSize().numberOfEmails)
    }

    @Test
    fun `Will return count of emails left to retry, when there are less than batch size emails left to retry`() {
        every { emailRepository.countEmailsSentInLastHour() } returns MAX_EMAILS_PER_HOUR - PENDING_EMAIL_BATCH_SIZE + 5

        assertEquals(PENDING_EMAIL_BATCH_SIZE - 5, emailQuota.getRetryFailedEmailsMaxBatchSize().numberOfEmails)
    }

    @Test
    fun `Will not set high priority flag for retry emails if normal limit is not hit`() {
        every { emailRepository.countEmailsSentInLastHour() } returns MAX_EMAILS_PER_HOUR - HIGH_PRIORITY_EMAIL_BUFFER - 1

        val batchSize = emailQuota.getRetryFailedEmailsMaxBatchSize()

        assertFalse(batchSize.highPriorityOnly)
    }

    @Test
    fun `Will set high priority flag for retry emails when normal limit is hit, but high priority buffer limit is not hit`() {
        every { emailRepository.countEmailsSentInLastHour() } returns MAX_EMAILS_PER_HOUR - HIGH_PRIORITY_EMAIL_BUFFER + 1

        val batchSize = emailQuota.getRetryFailedEmailsMaxBatchSize()

        assertTrue(batchSize.highPriorityOnly)
    }
}