package no.nav.arbeidsplassen.emailer.api.v1

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import no.nav.arbeidsplassen.emailer.azure.dto.MailContentType
import no.nav.arbeidsplassen.emailer.azure.impl.EmailServiceAzure
import org.slf4j.LoggerFactory

@Controller("/api/v1/sendmail")
class SendMailController(private val emailServiceAzure: EmailServiceAzure) {

    companion object {
        private val LOG = LoggerFactory.getLogger(SendMailController::class.java)
    }

    @Post("")
    fun sendMail(@PathVariable source: String, @Body email: EmailDTO): HttpResponse<String> {
        emailServiceAzure.sendSimpleMessage(email.recipient, email.subject,
            MailContentType.valueOf(email.type),email.content)
        return HttpResponse.created("Created")
    }
}
