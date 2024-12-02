package no.nav.arbeidsplassen.emailer.sendmail

import no.nav.arbeidsplassen.emailer.PostgresTestDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.test.assertEquals

@SpringBootTest
class OutboxEmailRepositoryTest : PostgresTestDatabase() {

    @Autowired
    lateinit var outboxEmailRepository: OutboxEmailRepository

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
}