package no.nav.arbeidsplassen.emailer.azure.impl

import com.microsoft.aad.adal4j.AuthenticationContext
import com.microsoft.aad.adal4j.AuthenticationResult
import com.microsoft.aad.adal4j.ClientCredential
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.client.RxHttpClient
import no.nav.arbeidsplassen.emailer.azure.dto.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.Executors
import javax.inject.Singleton

@Singleton
class EmailServiceAzure(private val aadProperties: AzureADProperties, val client: RxHttpClient) {
    private val sendEmailUri: String = aadProperties.resource + "/v1.0/users/" + aadProperties.userPrincipal + "/sendMail"

    companion object {
        private val LOG = LoggerFactory.getLogger(EmailServiceAzure::class.java)
    }

    @Volatile
    private var token: AADToken? = null

    fun sendSimpleMessage(to: String, subject: String, contentType: MailContentType, content: String) {
        val email = Email(Message(subject, Body(contentType, content), listOf(Recipient(Address(to.trim())))))
        try {
            sendMail(email)
        } catch (e: Exception) {
            throw SendMailException(e.message,e)
        }
    }

    private fun sendMail(email: Email) {
        renewTokenIfExpired()
        val postEmail = HttpRequest.POST(
            sendEmailUri,
            email).bearerAuth(token!!.accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON_TYPE)
        LOG.debug("sending mail using {}", aadProperties.resource)
        kotlin.runCatching {
            client.toBlocking().exchange(postEmail, String::class.java).status
        }.onSuccess { LOG.info("mail sent $it")}.onFailure { LOG.error("Got error", it) }
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
