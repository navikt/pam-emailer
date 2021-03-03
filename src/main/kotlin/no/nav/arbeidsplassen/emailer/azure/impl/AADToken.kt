package no.nav.arbeidsplassen.emailer.azure.impl

import java.time.LocalDateTime

class AADToken(val accessToken: String, val refreshToken: String?, val expires: LocalDateTime)

