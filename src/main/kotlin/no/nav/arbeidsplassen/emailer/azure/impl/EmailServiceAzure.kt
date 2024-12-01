package no.nav.arbeidsplassen.emailer.azure.impl

import com.azure.identity.ClientSecretCredentialBuilder
import com.microsoft.graph.core.authentication.AzureIdentityAuthenticationProvider
import com.microsoft.graph.core.requests.GraphClientFactory
import com.microsoft.graph.models.BodyType
import com.microsoft.graph.models.EmailAddress
import com.microsoft.graph.models.FileAttachment
import com.microsoft.graph.models.ItemBody
import com.microsoft.graph.models.Message
import com.microsoft.graph.models.Recipient
import com.microsoft.graph.models.odataerrors.ODataError
import com.microsoft.graph.serviceclient.GraphServiceClient
import com.microsoft.graph.users.item.sendmail.SendMailPostRequestBody
import no.nav.arbeidsplassen.emailer.sendmail.Email
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.temporal.ChronoUnit


@Service
class EmailServiceAzure(private val aadProperties: AzureADProperties) {
    companion object {
        private val LOG = LoggerFactory.getLogger(EmailServiceAzure::class.java)
        private val SECURE_LOG = LoggerFactory.getLogger(EmailServiceAzure::class.java.name + ".secure")
    }

    private final val graphClient = GraphServiceClient(
        AzureIdentityAuthenticationProvider(
            ClientSecretCredentialBuilder()
                .clientId(aadProperties.clientId)
                .clientSecret(aadProperties.clientSecret)
                .tenantId(aadProperties.tenantId)
                .build(),
            arrayOf<String>(),
            "https://graph.microsoft.com/.default"
        ),
        GraphClientFactory.create()
            .connectTimeout(Duration.of(20, ChronoUnit.SECONDS))
            .readTimeout(Duration.of(60, ChronoUnit.SECONDS))
            .build()
    )

    fun sendMail(email: Email, id: String) {
        val message = Message().apply {
            subject = email.subject
            body = ItemBody().apply {
                contentType = BodyType.forValue(email.type.lowercase())
                content = email.content
            }
            toRecipients = listOf(Recipient().apply { emailAddress = EmailAddress().apply { address = email.recipient.trim() } })
            attachments = email.attachments.map { FileAttachment().apply {
                name = it.name
                contentType = it.contentType
                contentBytes = it.content.toByteArray()
            } }
        }
        val emailRequestBody = SendMailPostRequestBody().apply {
            this.message = message
            saveToSentItems = false
        }

        try {
            sendMailWithRetry(emailRequestBody, id)
        } catch (e: SendMailException) {
            throw e
        } catch (e: Exception) {
            throw SendMailException(message = e.message, e = e)
        }
    }

    private fun sendMailWithRetry(email: SendMailPostRequestBody, id: String) {
        val maxRetries = 3
        var tryNo = 0
        var finished = false
        val httpErrors: MutableSet<Int> = mutableSetOf()

        while (!finished && tryNo < maxRetries) {
            tryNo++

            try {
                graphClient.users()
                    .byUserId(aadProperties.userPrincipal)
                    .sendMail()
                    .post(email)

                finished = true

            } catch (e: ODataError) {
                LOG.warn("Failed to send email with $id, try $tryNo. Response code ${e.responseStatusCode}. Wait and retry.")
                SECURE_LOG.warn("Failed to send email with $id. Response code ${e.responseStatusCode}. Message ${e.message}.", e)

                httpErrors.add(e.responseStatusCode)

                if (tryNo < maxRetries) {
                    Thread.sleep(3000L)
                }
            } catch (e: Exception) {
                LOG.error("Failed to send email with $id, try $tryNo. Unknown exception.", e)
                throw SendMailException(message = "Failed to send email with $id", e = e)
            }
        }

        if (!finished) {
            LOG.error("Fatal error for email with id $id. Gave up after $tryNo retries.")
            val status = if (httpErrors.size == 1) HttpStatus.valueOf(httpErrors.first()) else HttpStatus.INTERNAL_SERVER_ERROR
            throw SendMailException(message = "Fatal error for email with id $id", status = status)
        }
    }
}
