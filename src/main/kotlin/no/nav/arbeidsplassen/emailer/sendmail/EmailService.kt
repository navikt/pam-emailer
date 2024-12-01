package no.nav.arbeidsplassen.emailer.sendmail

import no.nav.arbeidsplassen.emailer.azure.impl.EmailServiceAzure
import org.springframework.stereotype.Service

@Service
class EmailService(private val emailServiceAzure: EmailServiceAzure) {

    fun sendEmail(email: Email, emailId: String, priority: Priority) {
        emailServiceAzure.sendMail(email, emailId)
    }

}
