package no.nav.arbeidsplassen.emailer.sendmail

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.arbeidsplassen.emailer.azure.impl.EmailServiceAzure
import no.nav.arbeidsplassen.emailer.azure.impl.SendMailException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class EmailService(
    private val emailRepository: OutboxEmailRepository,
    private val emailServiceAzure: EmailServiceAzure,
    private val objectMapper: ObjectMapper
) {
    private val limitHandler = LimitHandler(emailRepository)

    @Transactional
    fun sendNewEmail(email: Email, emailId: String, priority: Priority) {
        val outboxEmail = OutboxEmail.newOutboxEmail(emailId, priority, objectMapper.writeValueAsString(email))

        if (limitHandler.canSendEmailNow()) {
            try {
                emailServiceAzure.sendMail(email, emailId)
                outboxEmail.successfullySent()
            } catch (e: SendMailException) {
                outboxEmail.failedToSend()
            }
        }

        emailRepository.create(outboxEmail)
    }

    @Transactional
    fun sendEmail(outboxEmail: OutboxEmail) {
        try {
            val email = objectMapper.readValue(outboxEmail.payload, Email::class.java)
            emailServiceAzure.sendMail(email, outboxEmail.emailId)
            outboxEmail.successfullySent()
        } catch (e: SendMailException) {
            outboxEmail.failedToSend()
        }

        emailRepository.update(outboxEmail)
    }

}