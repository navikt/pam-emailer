package no.nav.arbeidsplassen.emailer.sendmail

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.arbeidsplassen.emailer.azure.impl.EmailServiceAzure
import no.nav.arbeidsplassen.emailer.azure.impl.SendMailException
import no.nav.arbeidsplassen.emailer.sendmail.EmailQuota.Companion.MAX_RETRIES
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class EmailService(
    private val emailRepository: OutboxEmailRepository,
    private val emailServiceAzure: EmailServiceAzure,
    private val objectMapper: ObjectMapper,
    private val emailQuota: EmailQuota,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(EmailService::class.java)
    }

    @Transactional
    fun sendNewEmail(email: Email, emailId: String, priority: Priority) {
        val outboxEmail = OutboxEmail.newOutboxEmail(emailId, priority, objectMapper.writeValueAsString(email))

        if (emailQuota.canSendEmailNow(outboxEmail)) {
            try {
                LOG.info("Sending email with id $emailId immediately")

                emailServiceAzure.sendMail(email, emailId)
                outboxEmail.successfullySent()

                LOG.info("Successfully sent email with id $emailId immediately")
            } catch (e: SendMailException) {
                outboxEmail.failedToSend()

                LOG.warn("Failed to send email with id $emailId immediately")
            }
        } else {
            LOG.info("No quota left for sending email with id $emailId immediately")
        }

        emailRepository.create(outboxEmail)
    }

    @Transactional
    fun sendEmail(outboxEmail: OutboxEmail) {
        try {
            LOG.info("Sending email with id ${outboxEmail.id}. Status: ${outboxEmail.status}. Try number: ${outboxEmail.retries}.")

            val email = objectMapper.readValue(outboxEmail.payload, Email::class.java)
            emailServiceAzure.sendMail(email, outboxEmail.emailId)
            outboxEmail.successfullySent()

            LOG.info("Successfully sent email with id ${outboxEmail.id}")
        } catch (e: SendMailException) {
            outboxEmail.failedToSend()
            LOG.warn("Failed to send email with id ${outboxEmail.id}")

            if (outboxEmail.retries >= MAX_RETRIES) {
                LOG.error("Failed to send email with id ${outboxEmail.id} after ${outboxEmail.retries} retries. Giving up.")
            }
        }

        emailRepository.update(outboxEmail)
    }
}