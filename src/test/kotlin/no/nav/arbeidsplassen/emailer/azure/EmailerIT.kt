package no.nav.arbeidsplassen.emailer.azure

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import no.nav.arbeidsplassen.emailer.azure.dto.Attachment
import no.nav.arbeidsplassen.emailer.azure.dto.MailContentType
import no.nav.arbeidsplassen.emailer.azure.impl.EmailServiceAzure
import org.junit.jupiter.api.Test
import java.util.*

@MicronautTest
class EmailerIT(private val emailServiceAzure: EmailServiceAzure) {

    @Test
    fun sendEmailAzure() {
        emailServiceAzure.sendSimpleMessage("recipient@somewhere.com", "Dette er en test",
            MailContentType.TEXT, "Dette er en test", UUID.randomUUID().toString());
    }

    @Test
    fun sendEmailAzureWithAttachment() {
        val encodeToByteArray: ByteArray = "Hei!".encodeToByteArray()
        val attachment = Attachment("test.txt", "plain/text", Base64.getEncoder().encodeToString(encodeToByteArray))

        emailServiceAzure.sendSimpleMessage("recipient@somewhere.com", "Dette er en test",
            MailContentType.TEXT, "Dette er en test", UUID.randomUUID().toString(), listOf(attachment));
    }
}
