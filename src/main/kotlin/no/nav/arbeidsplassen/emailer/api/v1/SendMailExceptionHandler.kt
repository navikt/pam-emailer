package no.nav.arbeidsplassen.emailer.api.v1

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Produces
import io.micronaut.http.server.exceptions.ExceptionHandler
import jakarta.inject.Singleton
import no.nav.arbeidsplassen.emailer.azure.impl.SendMailException
import org.slf4j.LoggerFactory


@Produces
@Singleton
class SendMailExceptionHandler : ExceptionHandler<SendMailException, HttpResponse<String>> {

    companion object {
        private val LOG = LoggerFactory.getLogger(SendMailExceptionHandler::class.java)
    }
    override fun handle(request: HttpRequest<*>, error: SendMailException): HttpResponse<String> {
        LOG.error("We got error while sending email", error)
        return HttpResponse.status(error.status, error.message)
    }
}
