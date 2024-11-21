package no.nav.arbeidsplassen.emailer.internal

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal")
class StatusController {

    @GetMapping("/isReady")
    fun isReady() = HttpStatus.OK

    @GetMapping("/isAlive")
    fun isAlive() = HttpStatus.OK

}
