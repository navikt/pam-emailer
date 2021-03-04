package no.nav.arbeidsplassen.emailer.internal

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import org.slf4j.LoggerFactory

@Controller("/internal")
class StatusController {

    companion object {
        private val LOG = LoggerFactory.getLogger(StatusController::class.java)
    }

    @Get("/isReady", produces = [MediaType.TEXT_PLAIN])
    fun isReady(): HttpResponse<String> {
        return HttpResponse.ok("OK")
    }

    @Get("/isAlive",produces = [MediaType.TEXT_PLAIN])
    fun isAlive(): HttpResponse<String> {
        return HttpResponse.ok("Alive")
    }
}
