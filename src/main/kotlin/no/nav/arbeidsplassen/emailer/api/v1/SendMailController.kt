package no.nav.arbeidsplassen.emailer.api.v1

import no.nav.arbeidsplassen.emailer.azure.impl.EmailServiceAzure
import no.nav.arbeidsplassen.emailer.azure.impl.SendMailException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/api/v1/sendmail")
class SendMailController(private val emailServiceAzure: EmailServiceAzure) {

    companion object {
        private val LOG = LoggerFactory.getLogger(SendMailController::class.java)
    }

    @PostMapping
    fun sendMail(@RequestBody email: EmailDTO): ResponseEntity<Void> {
        val id = if (email.identifier == null) {
            val identifier = UUID.randomUUID().toString()
            LOG.info("Got email request without identifier, adding new identifier: $identifier")
            identifier
        } else {
            LOG.info("Got email request with identifier: ${email.identifier}")
            email.identifier
        }

        emailServiceAzure.sendMail(email, id)

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @ExceptionHandler(SendMailException::class)
    fun handleSendMailException(exception: SendMailException): ResponseEntity<String> {
        LOG.error("We got error while sending email", exception)

        return ResponseEntity.status(exception.status).body(exception.message)
    }
}
