package no.nav.arbeidsplassen.emailer.azure

import io.micronaut.runtime.Micronaut

object Application {

    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.build()
            .packages("no.nav.arbeidsplassen.emailer")
            .mainClass(Application.javaClass)
            .start()
    }
}
