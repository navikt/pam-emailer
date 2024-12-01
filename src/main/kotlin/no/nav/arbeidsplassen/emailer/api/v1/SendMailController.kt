package no.nav.arbeidsplassen.emailer.api.v1

import no.nav.arbeidsplassen.emailer.azure.impl.SendMailException
import no.nav.arbeidsplassen.emailer.sendmail.Attachment
import no.nav.arbeidsplassen.emailer.sendmail.Email
import no.nav.arbeidsplassen.emailer.sendmail.EmailService
import no.nav.arbeidsplassen.emailer.sendmail.Priority
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


@RestController
@RequestMapping("/api/v1/sendmail")
class SendMailController(private val emailService: EmailService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(SendMailController::class.java)
    }

    @PostMapping
    @OptIn(ExperimentalEncodingApi::class)
    fun sendMail(@RequestBody emailDto: EmailDTO): ResponseEntity<Void> {
        val id = if (emailDto.identifier == null) {
            val identifier = UUID.randomUUID().toString()
            LOG.info("Got email request without identifier, adding new identifier: $identifier")
            identifier
        } else {
            LOG.info("Got email request with identifier: ${emailDto.identifier}")
            emailDto.identifier
        }

        val email = Email(
            recipient = emailDto.recipient,
            subject = emailDto.subject,
            content = emailDto.content,
            type = emailDto.type,
            attachments = emailDto.attachments.map { Attachment(it.name, it.contentType, Base64.Default.decode(it.base64Content).decodeToString()) }
        )

        emailService.sendEmail(email, id, Priority.NORMAL)

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @ExceptionHandler(SendMailException::class)
    fun handleSendMailException(exception: SendMailException): ResponseEntity<String> {
        LOG.error("We got error while sending email", exception)

        return ResponseEntity.status(exception.status).body(exception.message)
    }
}
