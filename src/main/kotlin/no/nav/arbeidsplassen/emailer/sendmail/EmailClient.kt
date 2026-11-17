package no.nav.arbeidsplassen.emailer.sendmail

interface EmailClient {
    fun sendEmail(email: Email, id: String)
}
