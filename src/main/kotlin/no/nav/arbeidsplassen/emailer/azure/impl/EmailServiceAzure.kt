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
        val emailRequestBody = createEmailRequestBody(email)

        try {
            graphClient.users()
                .byUserId(aadProperties.userPrincipal)
                .sendMail()
                .post(emailRequestBody)

        } catch (e: ODataError) {
            SECURE_LOG.warn("Failed to send email with $id. Response code ${e.responseStatusCode}. Message ${e.message}.", e)

            throw SendMailException(message = "Failed to send email with $id", status = HttpStatus.valueOf(e.responseStatusCode))
        } catch (e: Exception) {
            LOG.error("Failed to send email with $id. Unknown exception.", e)

            throw SendMailException(message = "Failed to send email with $id", e = e)
        }
    }

    private fun createEmailRequestBody(email: Email): SendMailPostRequestBody {
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
        return SendMailPostRequestBody().apply {
            this.message = message
            saveToSentItems = false
        }
    }

}
