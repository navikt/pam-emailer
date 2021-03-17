package no.nav.arbeidsplassen.emailer.api.v1

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import no.nav.arbeidsplassen.emailer.azure.dto.MailContentType
import no.nav.arbeidsplassen.emailer.azure.impl.EmailServiceAzure
import org.slf4j.LoggerFactory

@Controller("/api/v1/sendmail")
class SendMailController(private val emailServiceAzure: EmailServiceAzure) {

    companion object {
        private val LOG = LoggerFactory.getLogger(SendMailController::class.java)
    }

    @Post
    @ExecuteOn(TaskExecutors.IO)
    fun sendMail(@Body email: EmailDTO): HttpResponse<String> {
        LOG.info("Got email request with id: ${email.identifier}")
        emailServiceAzure.sendSimpleMessage(email.recipient, email.subject,
            MailContentType.valueOf(email.type),email.content)
        return HttpResponse.created("Created")
    }
}
