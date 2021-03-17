package no.nav.arbeidsplassen.emailer.azure

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import no.nav.arbeidsplassen.emailer.azure.dto.MailContentType
import no.nav.arbeidsplassen.emailer.azure.impl.EmailServiceAzure
import org.junit.jupiter.api.Test

@MicronautTest
class EmailerIT(private val emailServiceAzure: EmailServiceAzure) {

    @Test
    fun sendEmailAzure() {
        emailServiceAzure.sendSimpleMessage("recipient@somewhere.com", "Dette er en test",
            MailContentType.TEXT, "Dette er en test");
    }
}
