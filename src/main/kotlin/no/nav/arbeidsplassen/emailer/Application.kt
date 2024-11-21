package no.nav.arbeidsplassen.emailer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan("no.nav.arbeidsplassen.emailer")
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}