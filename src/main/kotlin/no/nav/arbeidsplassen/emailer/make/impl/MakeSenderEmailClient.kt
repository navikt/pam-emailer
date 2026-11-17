package no.nav.arbeidsplassen.emailer.make.impl

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.arbeidsplassen.emailer.sendmail.Email
import no.nav.arbeidsplassen.emailer.sendmail.EmailClient
import no.nav.arbeidsplassen.emailer.sendmail.SendMailException
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse
import java.nio.charset.StandardCharsets
import java.util.Base64

@Service
class MakeSenderEmailClient(
    private val properties: MakeSenderProperties,
    restClientBuilder: RestClient.Builder,
) : EmailClient {
    companion object {
        private val LOG = LoggerFactory.getLogger(MakeSenderEmailClient::class.java)
        private val secureLogsMarker: Marker = MarkerFactory.getMarker("TEAM_LOGS")
    }

    private val restClient: RestClient = restClientBuilder
        .baseUrl(requireNotNull(properties.baseUrl) { "makesender.base-url must be configured" })
        .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuthHeader())
        .build()

    override fun sendEmail(email: Email, id: String) {
        // Attachments are currently not supported, so we log a warning if there are any attachments in the email
        if (email.attachments.isNotEmpty()) {
            LOG.warn("Email with id $id has ${email.attachments.size} attachment(s) which are not supported by MakeSender and will be ignored")
        }

        val body = buildRequestBody(email)

        // exchange() skips the default status handling, and close = false keeps the response open so we can handle it ourselves
        val response = try {
            restClient.post()
                .uri("/api/v1/{domain}/messages", properties.domain)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange({ _, response -> response }, false)
        } catch (e: Exception) {
            LOG.error("Failed to send email. Email id: $id. Unknown exception.", e)
            throw SendMailException(message = "Failed to send email with $id", e = e)
        }

        response.use {
            handleSendMailResponse(it, id, email.recipient)
        }
    }

    private fun handleSendMailResponse(response: ConvertibleClientHttpResponse, id: String, recipient: String) {
        val statusCode = response.statusCode.value()
        val traceInfo = "Email id: $id. Status: $statusCode."
        val secureTraceInfo = "$traceInfo Recipient: $recipient."

        if (response.statusCode.isError) {
            val errorBody = runCatching { response.body.readAllBytes().toString(StandardCharsets.UTF_8) }.getOrNull()

            LOG.error("Failed to send email. $traceInfo")
            LOG.error(secureLogsMarker, "Failed to send email. $secureTraceInfo Response body: $errorBody")

            throw SendMailException(
                message = "Failed to send email with $id",
                status = HttpStatus.resolve(statusCode) ?: HttpStatus.INTERNAL_SERVER_ERROR
            )
        }

        // The email is accepted at this point, so a parse failure must not make the caller retry and send it again
        val makeSenderResponse = runCatching { response.bodyTo(MakeSenderResponse::class.java) }.getOrNull()
        val makeSenderInfo = "MakeSender message: ${makeSenderResponse?.message}. makeSenderId: ${makeSenderResponse?.id}."

        LOG.info("Email sent to MakeSender. $traceInfo $makeSenderInfo")
        LOG.info(secureLogsMarker, "Email sent to MakeSender. $secureTraceInfo $makeSenderInfo")
    }

    private fun buildRequestBody(email: Email): Map<String, String> {
        val body = mutableMapOf(
            "from" to requireNotNull(properties.sender) { "makesender.sender must be configured" },
            "to" to email.recipient.trim(),
            "subject" to email.subject,
        )

        if (email.type.equals("html", ignoreCase = true)) {
            body["html"] = email.content
        } else {
            body["text"] = email.content
        }

        return body
    }

    private fun basicAuthHeader(): String {
        val credentials = "${properties.userId}:${properties.apiKey}"
        val encoded = Base64.getEncoder().encodeToString(credentials.toByteArray(StandardCharsets.UTF_8))
        return "Basic $encoded"
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class MakeSenderResponse(
    val id: Long?,
    val message: String?,
)
