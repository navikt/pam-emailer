package no.nav.arbeidsplassen.emailer.make.impl

import no.nav.arbeidsplassen.emailer.sendmail.Email
import no.nav.arbeidsplassen.emailer.sendmail.EmailClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MakeSenderEmailClient(private val properties: MakeSenderProperties) : EmailClient {
    companion object {
        private val LOG = LoggerFactory.getLogger(MakeSenderEmailClient::class.java)
    }

    override fun sendEmail(email: Email, id: String) {
        // TODO: Build JSON body from Email and POST to
        //  {baseUrl}/api/v1/{domain}/messages with Basic auth (userId:apiKey).
        //  On non-2xx, throw SendMailException(status = ...).
        LOG.error("MakeSenderEmailClient.sendEmail called for email with id $id (not yet implemented)")
    }
}
