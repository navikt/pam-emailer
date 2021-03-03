package no.nav.arbeidsplassen.emailer.azure.impl

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("azure.ad")
class AzureADProperties (
    var tenantId: String? = null,
    var authority: String? = null,
    var clientId: String? = null,
    var clientSecret: String? = null,
    var resource: String? = null,
    var userPrincipal: String? = null
)
