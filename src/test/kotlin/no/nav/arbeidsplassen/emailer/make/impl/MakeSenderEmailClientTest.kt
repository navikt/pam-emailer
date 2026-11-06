package no.nav.arbeidsplassen.emailer.make.impl

import no.nav.arbeidsplassen.emailer.sendmail.Email
import no.nav.arbeidsplassen.emailer.sendmail.Priority
import no.nav.arbeidsplassen.emailer.sendmail.SendMailException
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestClient
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MakeSenderEmailClientTest {

    private val properties = MakeSenderProperties(
        baseUrl = "https://api.makesender.test",
        domain = "arbeidsplassen.nav.no",
        userId = "user",
        apiKey = "key",
        sender = "no-reply@arbeidsplassen.nav.no",
    )

    private val builder = RestClient.builder()
    private val server: MockRestServiceServer = MockRestServiceServer.bindTo(builder).build()
    private val client = MakeSenderEmailClient(properties, builder)

    private fun email(type: String = "html", attachments: List<no.nav.arbeidsplassen.emailer.sendmail.Attachment> = listOf()) =
        Email(
            recipient = " recipient@example.com ",
            subject = "Subject",
            content = "<p>Hello</p>",
            priority = Priority.NORMAL,
            type = type,
            attachments = attachments,
        )

    @Test
    fun `posts expected body with basic auth and succeeds on 2xx`() {
        val expectedAuth = "Basic " + Base64.getEncoder().encodeToString("user:key".toByteArray())

        server.expect(ExpectedCount.once(), requestTo("https://api.makesender.test/api/v1/arbeidsplassen.nav.no/messages"))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, expectedAuth))
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.from").value("no-reply@arbeidsplassen.nav.no"))
            .andExpect(jsonPath("$.to").value("recipient@example.com"))
            .andExpect(jsonPath("$.subject").value("Subject"))
            .andExpect(jsonPath("$.html").value("<p>Hello</p>"))
            .andRespond(withStatus(HttpStatus.OK))

        client.sendEmail(email(), "id-1")

        server.verify()
    }

    @Test
    fun `uses text field for non-html type`() {
        server.expect(requestTo("https://api.makesender.test/api/v1/arbeidsplassen.nav.no/messages"))
            .andExpect(jsonPath("$.text").value("<p>Hello</p>"))
            .andRespond(withStatus(HttpStatus.OK))

        client.sendEmail(email(type = "text"), "id-2")

        server.verify()
    }

    @Test
    fun `throws SendMailException with status on error response`() {
        server.expect(requestTo("https://api.makesender.test/api/v1/arbeidsplassen.nav.no/messages"))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST))

        val exception = assertFailsWith<SendMailException> {
            client.sendEmail(email(), "id-3")
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.status)
        server.verify()
    }
}
