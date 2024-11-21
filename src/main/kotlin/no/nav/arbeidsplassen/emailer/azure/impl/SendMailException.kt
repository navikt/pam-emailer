package no.nav.arbeidsplassen.emailer.azure.impl

import org.springframework.http.HttpStatus

class SendMailException(
    message: String?,
    val status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    e: Throwable? = null
) : Throwable(message, e) {
    override fun toString(): String {
        return "${super.toString()}, status code: ${status.value()} ($status)"
    }
}

