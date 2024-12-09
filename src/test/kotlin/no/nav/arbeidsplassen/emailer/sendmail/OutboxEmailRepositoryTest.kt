package no.nav.arbeidsplassen.emailer.sendmail

import no.nav.arbeidsplassen.emailer.PostgresTestDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest
class OutboxEmailRepositoryTest : PostgresTestDatabase() {

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var outboxEmailRepository: OutboxEmailRepository

    @BeforeEach
    fun cleanDatabase() {
        jdbcTemplate.update("DELETE FROM outbox_email")
    }

    @Test
    fun `E-mails are stored and read correctly`() {
        val email = OutboxEmail(
            id = UUID.randomUUID(),
            emailId = UUID.randomUUID().toString(),
            status = Status.PENDING,
            priority = Priority.NORMAL,
            createdAt = OffsetDateTime.now().minusHours(1),
            updatedAt = OffsetDateTime.now(),
            retries = 0,
            payload = "payload"
        )

        outboxEmailRepository.create(email)

        val storedEmail = outboxEmailRepository.findById(email.id)!!

        assertEquals(email.id, storedEmail.id)
        assertEquals(email.emailId, storedEmail.emailId)
        assertEquals(email.status, storedEmail.status)
        assertEquals(email.priority, storedEmail.priority)
        assertThat(email.createdAt.truncatedTo(ChronoUnit.MILLIS)).isEqualTo(storedEmail.createdAt.truncatedTo(ChronoUnit.MILLIS))
        assertThat(email.updatedAt.truncatedTo(ChronoUnit.MILLIS)).isEqualTo(storedEmail.updatedAt.truncatedTo(ChronoUnit.MILLIS))
        assertEquals(email.retries, storedEmail.retries)
        assertEquals(email.payload, storedEmail.payload)
    }

    @Test
    fun `Only sent e-mails in the last hours count as sent`() {
        val pendingEmail = OutboxEmail.newOutboxEmail(UUID.randomUUID().toString(), Priority.NORMAL, "payload")
        val sentEmailInLastHour = pendingEmail.copy(id = UUID.randomUUID(), status = Status.SENT, updatedAt = OffsetDateTime.now().minusMinutes(45))

        outboxEmailRepository.create(pendingEmail)
        outboxEmailRepository.create(sentEmailInLastHour)

        val emailsSentInLastHour = outboxEmailRepository.countEmailsSentInLastHour()

        assertEquals(1, emailsSentInLastHour)

        val sentEmailMoreThanAnHourAgo = pendingEmail.copy(id = UUID.randomUUID(), status = Status.SENT, updatedAt = OffsetDateTime.now().minusMinutes(65))
        val failedEmail = pendingEmail.copy(id = UUID.randomUUID(), status = Status.FAILED, updatedAt = OffsetDateTime.now().minusMinutes(45))

        outboxEmailRepository.create(sentEmailMoreThanAnHourAgo)
        outboxEmailRepository.create(failedEmail)

        val emailsSentInLastHour2 = outboxEmailRepository.countEmailsSentInLastHour()

        assertEquals(1, emailsSentInLastHour2)
    }

    @Test
    fun `Pending emails are returned with oldest first from find pending`() {
        val pendingEmail = OutboxEmail.newOutboxEmail(UUID.randomUUID().toString(), Priority.NORMAL, "payload")
        val pendingEmailOlder = pendingEmail.copy(id = UUID.randomUUID(), createdAt = OffsetDateTime.now().minusHours(1))
        val pendingEmailOldest = pendingEmail.copy(id = UUID.randomUUID(), createdAt = OffsetDateTime.now().minusHours(2))
        val sentEmail = pendingEmail.copy(id = UUID.randomUUID(), status = Status.SENT)
        val failedEmail = pendingEmail.copy(id = UUID.randomUUID(), status = Status.FAILED)

        outboxEmailRepository.create(pendingEmail)
        outboxEmailRepository.create(pendingEmailOlder)
        outboxEmailRepository.create(pendingEmailOldest)
        outboxEmailRepository.create(sentEmail)
        outboxEmailRepository.create(failedEmail)

        val pendingEmails = outboxEmailRepository.findPendingSortedByPriorityAndCreated(2, false)

        assertThat(pendingEmails)
            .extracting("id")
            .contains(pendingEmailOlder.id, pendingEmailOldest.id)

        assertThat(pendingEmails)
            .extracting("id")
            .doesNotContain(pendingEmail.id, sentEmail.id, failedEmail.id)
    }

    @Test
    fun `Pending emails will only return high priority emails when flag is set`() {
        val pendingEmailNormalPriority = OutboxEmail.newOutboxEmail(UUID.randomUUID().toString(), Priority.NORMAL, "payload")
        val pendingEmailHighPriority = pendingEmailNormalPriority.copy(id = UUID.randomUUID(), priority = Priority.HIGH, updatedAt = OffsetDateTime.now().minusMinutes(20))

        outboxEmailRepository.create(pendingEmailNormalPriority)
        outboxEmailRepository.create(pendingEmailHighPriority)

        val pendingEmails = outboxEmailRepository.findPendingSortedByPriorityAndCreated(10, true)

        assertEquals(1, pendingEmails.size)

        assertEquals(pendingEmailHighPriority.id, pendingEmails.first().id)
    }

    @Test
    fun `High priority are returned before normal priority from find pending`() {
        val pendingEmailNormalPriority = OutboxEmail.newOutboxEmail(UUID.randomUUID().toString(), Priority.NORMAL, "payload")
        val pendingEmailOlderHighPriority = pendingEmailNormalPriority.copy(id = UUID.randomUUID(), priority = Priority.HIGH, createdAt = OffsetDateTime.now().minusHours(1))
        val pendingEmailOldestNormalPriority = pendingEmailNormalPriority.copy(id = UUID.randomUUID(), createdAt = OffsetDateTime.now().minusHours(2))

        outboxEmailRepository.create(pendingEmailNormalPriority)
        outboxEmailRepository.create(pendingEmailOlderHighPriority)
        outboxEmailRepository.create(pendingEmailOldestNormalPriority)

        val pendingEmails = outboxEmailRepository.findPendingSortedByPriorityAndCreated(10, false)

        assertEquals(pendingEmailOlderHighPriority.id, pendingEmails[0].id)
        assertEquals(pendingEmailOldestNormalPriority.id, pendingEmails[1].id)
        assertEquals(pendingEmailNormalPriority.id, pendingEmails[2].id)
    }

    @Test
    fun `Failed emails are returned with oldest first from find failed`() {
        val pendingEmail = OutboxEmail.newOutboxEmail(UUID.randomUUID().toString(), Priority.NORMAL, "payload")
        val failedEmail = pendingEmail.copy(id = UUID.randomUUID(), status = Status.FAILED, updatedAt = OffsetDateTime.now().minusMinutes(10))
        val failedEmailOlder = pendingEmail.copy(id = UUID.randomUUID(), status = Status.FAILED, updatedAt = OffsetDateTime.now().minusMinutes(20))
        val failedEmailOldest = pendingEmail.copy(id = UUID.randomUUID(), status = Status.FAILED, updatedAt = OffsetDateTime.now().minusMinutes(30))
        val sentEmail = pendingEmail.copy(id = UUID.randomUUID(), status = Status.SENT)

        outboxEmailRepository.create(pendingEmail)
        outboxEmailRepository.create(failedEmail)
        outboxEmailRepository.create(failedEmailOlder)
        outboxEmailRepository.create(failedEmailOldest)
        outboxEmailRepository.create(sentEmail)

        val failedEmails = outboxEmailRepository.findFailedSortedByPriorityAndUpdated(2, false)

        assertEquals(2, failedEmails.size)

        assertEquals(failedEmailOldest.id, failedEmails[0].id)
        assertEquals(failedEmailOlder.id, failedEmails[1].id)
    }

    @Test
    fun `Failed emails will only return high priority emails when flag is set`() {
        val failedEmailNormalPriority = OutboxEmail.newOutboxEmail(UUID.randomUUID().toString(), Priority.NORMAL, "payload").apply { status = Status.FAILED }
        val failedEmailHighPriority = failedEmailNormalPriority.copy(id = UUID.randomUUID(), priority = Priority.HIGH, updatedAt = OffsetDateTime.now().minusMinutes(20))

        outboxEmailRepository.create(failedEmailNormalPriority)
        outboxEmailRepository.create(failedEmailHighPriority)

        val failedEmails = outboxEmailRepository.findFailedSortedByPriorityAndUpdated(10, true)

        assertEquals(1, failedEmails.size)

        assertEquals(failedEmailHighPriority.id, failedEmails.first().id)
    }

    @Test
    fun `High priority are returned before normal priority from find failed`() {
        val failedEmailNormalPriority = OutboxEmail.newOutboxEmail(UUID.randomUUID().toString(), Priority.NORMAL, "payload").apply { status = Status.FAILED }
        val failedEmailOlderHighPriority = failedEmailNormalPriority.copy(id = UUID.randomUUID(), priority = Priority.HIGH, updatedAt = OffsetDateTime.now().minusHours(1))
        val failedEmailOldestNormalPriority = failedEmailNormalPriority.copy(id = UUID.randomUUID(), updatedAt = OffsetDateTime.now().minusHours(2))

        outboxEmailRepository.create(failedEmailNormalPriority)
        outboxEmailRepository.create(failedEmailOlderHighPriority)
        outboxEmailRepository.create(failedEmailOldestNormalPriority)

        val failedEmails = outboxEmailRepository.findFailedSortedByPriorityAndUpdated(10, false)

        assertEquals(failedEmailOlderHighPriority.id, failedEmails[0].id)
        assertEquals(failedEmailOldestNormalPriority.id, failedEmails[1].id)
        assertEquals(failedEmailNormalPriority.id, failedEmails[2].id)
    }

    @Test
    fun `Normal priority emails are retried 1 times`() {
        val emailBelowRetryLimit = OutboxEmail.newOutboxEmail(UUID.randomUUID().toString(), Priority.NORMAL, "payload")
        emailBelowRetryLimit.failedToSend()

        val emailAboveRetryLimit = OutboxEmail.newOutboxEmail(UUID.randomUUID().toString(), Priority.NORMAL, "payload")
        emailAboveRetryLimit.failedToSend() // Failed first, "normal" attempt
        emailAboveRetryLimit.failedToSend() // Failed retry

        outboxEmailRepository.create(emailBelowRetryLimit)
        outboxEmailRepository.create(emailAboveRetryLimit)

        val failedEmails = outboxEmailRepository.findFailedSortedByPriorityAndUpdated(10, false)

        assertEquals(1, failedEmails.size)
        assertEquals(emailBelowRetryLimit.id, failedEmails[0].id)
    }

    @Test
    fun `High priority emails are retried 50 times`() {
        val emailBelowRetryLimit = OutboxEmail.newOutboxEmail(UUID.randomUUID().toString(), Priority.HIGH, "payload")
        emailBelowRetryLimit.failedToSend()
        emailBelowRetryLimit.retries = 49

        val emailAboveRetryLimit = emailBelowRetryLimit.copy(id = UUID.randomUUID(), retries = 50)

        outboxEmailRepository.create(emailBelowRetryLimit)
        outboxEmailRepository.create(emailAboveRetryLimit)

        val failedEmails = outboxEmailRepository.findFailedSortedByPriorityAndUpdated(10, false)

        assertEquals(1, failedEmails.size)
        assertEquals(emailBelowRetryLimit.id, failedEmails[0].id)
    }

    @Test
    fun `Emails not sent more than an hour ago will not be deleted`() {
        val pendingEmail = OutboxEmail.newOutboxEmail(UUID.randomUUID().toString(), Priority.NORMAL, "payload")
        val sentEmail = OutboxEmail.newOutboxEmail(UUID.randomUUID().toString(), Priority.NORMAL, "payload").apply { status = Status.SENT }
        val failed = OutboxEmail.newOutboxEmail(UUID.randomUUID().toString(), Priority.NORMAL, "payload").apply { status = Status.FAILED }

        outboxEmailRepository.create(pendingEmail)
        outboxEmailRepository.create(sentEmail)
        outboxEmailRepository.create(failed)

        outboxEmailRepository.deleteEmailsOlderThanAnHour()

        assertNotNull(outboxEmailRepository.findById(pendingEmail.id))
        assertNotNull(outboxEmailRepository.findById(sentEmail.id))
        assertNotNull(outboxEmailRepository.findById(failed.id))
    }

    @Test
    fun `Only emails sent more than an hour ago are deleted with`() {
        val pendingEmail = OutboxEmail.newOutboxEmail(UUID.randomUUID().toString(), Priority.NORMAL, "payload")
            .apply { updatedAt = OffsetDateTime.now().minusHours(2) }
        val sentEmail = OutboxEmail.newOutboxEmail(UUID.randomUUID().toString(), Priority.NORMAL, "payload")
            .apply { status = Status.SENT; updatedAt = OffsetDateTime.now().minusHours(2) }
        val failed = OutboxEmail.newOutboxEmail(UUID.randomUUID().toString(), Priority.NORMAL, "payload")
            .apply { status = Status.FAILED; updatedAt = OffsetDateTime.now().minusHours(2) }

        outboxEmailRepository.create(pendingEmail)
        outboxEmailRepository.create(sentEmail)
        outboxEmailRepository.create(failed)

        outboxEmailRepository.deleteEmailsOlderThanAnHour()

        assertNotNull(outboxEmailRepository.findById(pendingEmail.id))
        assertNull(outboxEmailRepository.findById(sentEmail.id))
        assertNotNull(outboxEmailRepository.findById(failed.id))
    }
}