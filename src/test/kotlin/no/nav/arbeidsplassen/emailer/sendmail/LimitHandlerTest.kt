package no.nav.arbeidsplassen.emailer.sendmail

import io.mockk.every
import io.mockk.mockk
import no.nav.arbeidsplassen.emailer.sendmail.LimitHandler.Companion.MAX_EMAILS_PER_HOUR
import no.nav.arbeidsplassen.emailer.sendmail.LimitHandler.Companion.PENDING_EMAIL_BATCH_SIZE
import no.nav.arbeidsplassen.emailer.sendmail.LimitHandler.Companion.RETRY_EMAIL_BATCH_SIZE
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LimitHandlerTest {

    private val emailRepository = mockk<OutboxEmailRepository>()
    private val limitHandler: LimitHandler = LimitHandler(emailRepository)

    @Test
    fun `Can send emails when not hitting limit`() {
        every { emailRepository.countEmailsSentInLastHour() } returns 0

        assertTrue(limitHandler.canSendEmailNow())
    }

    @Test
    fun `Can not send emails when limit is hit`() {
        every { emailRepository.countEmailsSentInLastHour() } returns MAX_EMAILS_PER_HOUR + 1

        assertFalse(limitHandler.canSendEmailNow())
    }

    @Test
    fun `Will return pending email batch size, when there are more emails than batch size left to send`() {
        every { emailRepository.countEmailsSentInLastHour() } returns MAX_EMAILS_PER_HOUR - PENDING_EMAIL_BATCH_SIZE - 10

        assertEquals(PENDING_EMAIL_BATCH_SIZE, limitHandler.emailsToSend())
    }

    @Test
    fun `Will return 0 emails to send when there are no more emails to send`() {
        every { emailRepository.countEmailsSentInLastHour() } returns MAX_EMAILS_PER_HOUR

        assertEquals(0, limitHandler.emailsToSend())
    }

    @Test
    fun `Will return count of emails left to send, when there are less than batch size emails left to send`() {
        every { emailRepository.countEmailsSentInLastHour() } returns MAX_EMAILS_PER_HOUR - PENDING_EMAIL_BATCH_SIZE + 5

        assertEquals(PENDING_EMAIL_BATCH_SIZE - 5, limitHandler.emailsToSend())
    }

    @Test
    fun `Will return retry email batch size, when there are more emails than batch size left to send`() {
        every { emailRepository.countEmailsSentInLastHour() } returns MAX_EMAILS_PER_HOUR - RETRY_EMAIL_BATCH_SIZE - 10

        assertEquals(RETRY_EMAIL_BATCH_SIZE, limitHandler.emailsToRetry())
    }

    @Test
    fun `Will return 0 emails to send when there are no more emails to retry`() {
        every { emailRepository.countEmailsSentInLastHour() } returns MAX_EMAILS_PER_HOUR

        assertEquals(0, limitHandler.emailsToRetry())
    }

    @Test
    fun `Will return count of emails left to retry, when there are less than batch size emails left to retry`() {
        every { emailRepository.countEmailsSentInLastHour() } returns MAX_EMAILS_PER_HOUR - PENDING_EMAIL_BATCH_SIZE + 5

        assertEquals(PENDING_EMAIL_BATCH_SIZE - 5, limitHandler.emailsToRetry())
    }
}