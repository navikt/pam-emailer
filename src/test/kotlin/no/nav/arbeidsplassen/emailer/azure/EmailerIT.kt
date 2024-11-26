package no.nav.arbeidsplassen.emailer.azure

import no.nav.arbeidsplassen.emailer.api.v1.AttachmentDto
import no.nav.arbeidsplassen.emailer.api.v1.EmailDTO
import no.nav.arbeidsplassen.emailer.azure.impl.EmailServiceAzure
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.*

@Disabled
@SpringBootTest
class EmailerIT {

    @Autowired
    private lateinit var emailServiceAzure: EmailServiceAzure

    @Test
    fun sendEmailAzure() {
        val email = EmailDTO(
            identifier = UUID.randomUUID().toString(),
            recipient = "recipient@somewhere.com",
            subject = "Dette er en test",
            content = "Dette er en test content",
            type = "TEXT"
        )

        emailServiceAzure.sendMail(email, "hei hei")
    }

    @Test
    fun sendEmailAzureWithAttachment() {
        val email = EmailDTO(
            identifier = UUID.randomUUID().toString(),
            recipient = "recipient@somewhere.com",
            subject = "Dette er en test",
            content = "Dette er en test content",
            type = "TEXT",
            attachments = listOf(AttachmentDto("test.txt", "plain/text", Base64.getEncoder().encodeToString("Hei!".encodeToByteArray())))
        )

        emailServiceAzure.sendMail(email, "hei hei")
    }
}
