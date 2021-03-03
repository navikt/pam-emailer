package no.nav.arbeidsplassen.emailer.api.v1

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Produces
import io.micronaut.http.server.exceptions.ExceptionHandler
import no.nav.arbeidsplassen.emailer.azure.impl.SendMailException
import javax.inject.Singleton

@Produces
@Singleton
class SendMailExceptionHandler : ExceptionHandler<SendMailException, HttpResponse<String>> {

    override fun handle(request: HttpRequest<*>, error: SendMailException):
            HttpResponse<String> = HttpResponse.serverError(error.message)
}
