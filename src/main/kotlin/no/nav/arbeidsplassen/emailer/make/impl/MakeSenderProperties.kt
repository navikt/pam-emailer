package no.nav.arbeidsplassen.emailer.make.impl

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("makesender")
class MakeSenderProperties(
    var baseUrl: String? = null,
    var domain: String? = null,
    var userId: String? = null,
    var apiKey: String? = null,
    var sender: String? = null,
)
