package no.nav.arbeidsplassen.emailer.api.v1

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.arbeidsplassen.emailer.sendmail.Email
import no.nav.arbeidsplassen.emailer.sendmail.EmailService
import no.nav.arbeidsplassen.emailer.sendmail.Priority
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

class SendMailControllerTest {

    private val emailService = mockk<EmailService>(relaxed = true)
    private val controller = SendMailController(emailService)

    private fun emailDto(priority: String?) = EmailDTO(
        identifier = UUID.randomUUID().toString(),
        recipient = "recipient@somewhere.com",
        subject = "subject",
        content = "content",
        type = "TEXT",
        priority = priority
    )

    private fun capturePriority(dto: EmailDTO): Priority {
        val priority = slot<Priority>()
        every { emailService.sendNewEmail(any(), any(), capture(priority)) } returns Unit

        controller.sendMail(dto)

        verify(exactly = 1) { emailService.sendNewEmail(any(), any(), any()) }
        return priority.captured
    }

    @Test
    fun `HIGH priority from the client is passed on to the outbox`() {
        assertEquals(Priority.HIGH, capturePriority(emailDto("high")))
    }

    @Test
    fun `NORMAL priority from the client is passed on to the outbox`() {
        assertEquals(Priority.NORMAL, capturePriority(emailDto("normal")))
    }

    @Test
    fun `Priority is matched case insensitively`() {
        assertEquals(Priority.HIGH, capturePriority(emailDto("HIGH")))
    }

    @Test
    fun `Missing priority falls back to NORMAL`() {
        assertEquals(Priority.NORMAL, capturePriority(emailDto(null)))
    }

    @Test
    fun `Unknown priority falls back to NORMAL`() {
        assertEquals(Priority.NORMAL, capturePriority(emailDto("urgent")))
    }

    @Test
    fun `Priority on the payload matches the priority passed to the outbox`() {
        val email = slot<Email>()
        val priority = slot<Priority>()
        every { emailService.sendNewEmail(capture(email), any(), capture(priority)) } returns Unit

        controller.sendMail(emailDto("high"))

        assertEquals(email.captured.priority, priority.captured)
    }
}
