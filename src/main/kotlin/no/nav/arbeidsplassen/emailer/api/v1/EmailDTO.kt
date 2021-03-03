package no.nav.arbeidsplassen.emailer.api.v1

data class EmailDTO(val recipient: String, val subject: String, val content: String, val type: String)

