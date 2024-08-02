package no.nav.arbeidsplassen.emailer.azure.impl

import io.micronaut.http.HttpStatus
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class SendMailExceptionTest {

    @Test
    fun `Status code should be part of the printed stacktrace`() {
        val expectedStatusCode = HttpStatus.BAD_GATEWAY
        val cause = RuntimeException("Simulert feil i en test")
        val exception = SendMailException("Dette er meldigen", status = expectedStatusCode, e = cause)

        assertTrue(exception.toString().contains(expectedStatusCode.toString()))
        assertTrue(exception.stackTraceToString().contains(expectedStatusCode.toString()))
    }
}
