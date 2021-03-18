package no.nav.arbeidsplassen.emailer.azure.impl

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.microsoft.aad.adal4j.AuthenticationContext
import com.microsoft.aad.adal4j.AuthenticationResult
import com.microsoft.aad.adal4j.ClientCredential
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.client.annotation.Client
import no.nav.arbeidsplassen.emailer.azure.dto.*
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.Executors
import javax.inject.Singleton

@Singleton
class EmailServiceAzure(private val aadProperties: AzureADProperties, @Client("SendMail") val client: RxHttpClient) {
    private val sendEmailUri: String = aadProperties.resource + "/v1.0/users/" + aadProperties.userPrincipal + "/sendMail"

    companion object {
        private val LOG = LoggerFactory.getLogger(EmailServiceAzure::class.java)
    }
    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule())
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }


    @Volatile
    private var token: AADToken? = null


    fun sendSimpleMessage(to: String, subject: String, contentType: MailContentType, content: String, id: String) {
        val email = Email(Message(subject, Body(contentType, content), listOf(Recipient(Address(to.trim())))))
        try {
            sendMailUsingURLConnection(email, id)
//            sendMail(email, id)
        } catch (e: Exception) {
            throw SendMailException(e.message,e)
        }
    }

    private fun sendMail(email: Email, id: String) {
        renewTokenIfExpired()
        val postEmail = HttpRequest.POST(
            sendEmailUri,
            email).bearerAuth(token!!.accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON_TYPE)
        LOG.debug("sending mail using {}", aadProperties.resource)
        kotlin.runCatching {
            client.exchange(postEmail, String::class.java).blockingFirst()
        }.onSuccess { LOG.info("mail sent $id")}.onFailure { LOG.error("Got error $id", it) }
    }

    private fun renewTokenIfExpired() {
        val inTwoMinutes = LocalDateTime.now().plusMinutes(2L)
        if (token == null || token!!.expires.isBefore(inTwoMinutes)) {
            synchronized(this) {
                if (token == null || token!!.expires.isBefore(inTwoMinutes)) {
                    LOG.info("Renewing Azure token")
                    token = aADToken
                    LOG.info("New token expires at {}", token!!.expires)
                }
            }
        }
    }

    private fun sendMailUsingURLConnection(email: Email, id: String) {
        renewTokenIfExpired()
        val emailAsJson = objectMapper.writeValueAsString(email)
        val (responseCode, responseBody) = with(URL(sendEmailUri).openConnection() as HttpURLConnection) {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 60000
            useCaches = false

            setRequestProperty("Authorization", "Bearer ${token!!.accessToken}")
            setRequestProperty("Content-Type", MediaType.APPLICATION_JSON)
            setRequestProperty("Accept", MediaType.APPLICATION_JSON)
            doOutput = true

            outputStream.writer(Charsets.UTF_8).apply {
                write(emailAsJson)
                flush()
            }

            val stream: InputStream? = if (responseCode < 300) this.inputStream else this.errorStream
            responseCode to stream?.use{ s->s.bufferedReader()?.readText()}
        }

        if (responseCode >= 300 || responseBody == null) {
            LOG.error("Got error $responseCode for $id")
        } else {
            LOG.info("mail sent $id")
        }

    }
    private val aADToken: AADToken
        get() {
            val service = Executors.newSingleThreadExecutor()
            LOG.debug("Connecting using authority: {}", aadProperties.authority)
            val context = AuthenticationContext(aadProperties.authority, true, service)
            val future = context.acquireToken(
                aadProperties.resource, ClientCredential(
                    aadProperties.clientId, aadProperties.clientSecret
                ), null
            )
            val result: AuthenticationResult = future.get()
            return AADToken(
                result.accessToken, result.refreshToken, result.expiresOnDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
            )
        }
}
