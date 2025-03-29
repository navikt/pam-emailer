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
import com.microsoft.graph.users.item.messages.MessagesRequestBuilder
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

    fun deleteAllEmailsInAccount() {
        val totalEmailCount = getTotalEmailCount()

        if (totalEmailCount == 0) {
            return
        }

        LOG.info("Found $totalEmailCount emails that will be deleted")

        var deletedEmailsCount = 0
        while (true) {
            val emails = getEmails()

            if (emails.size == 0) {
                return
            }

            emails.forEach { email ->
                deleteEmail(email.id)
                deletedEmailsCount++
            }

            LOG.info("$deletedEmailsCount of $totalEmailCount emails deleted")
        }
    }

    private fun getTotalEmailCount() = graphClient.users()
        .byUserId(aadProperties.userPrincipal)
        .mailFolders()
        .get()
        .value
        .sumOf { it.totalItemCount }

    private fun getEmails() =
        graphClient.users()
            .byUserId(aadProperties.userPrincipal)
            .messages()[{ requestConfiguration: MessagesRequestBuilder.GetRequestConfiguration ->
            requestConfiguration.queryParameters.select = arrayOf("id")
            requestConfiguration.queryParameters.top = 100
        }]
            .value

    private fun deleteEmail(emailId: String) {
        try {
            graphClient.users()
                .byUserId(aadProperties.userPrincipal)
                .messages()
                .byMessageId(emailId)
                .permanentDelete()
                .post()
        } catch (e: ODataError) {
            LOG.error("Failed to delete email with id $emailId. Response code ${e.responseStatusCode}. Code: ${e.error.code}. Message ${e.message}.", e)
            throw e
        } catch (e: Exception) {
            LOG.error("Failed to delete email with $emailId. Unknown exception.", e)
            throw e
        }
    }

    fun sendMail(email: Email, id: String) {
        val emailRequestBody = createEmailRequestBody(email)

        try {
            graphClient.users()
                .byUserId(aadProperties.userPrincipal)
                .sendMail()
                .post(emailRequestBody)

        } catch (e: ODataError) {
            SECURE_LOG.warn("Failed to send email with $id. Response code ${e.responseStatusCode}. Code: ${e.error.code}. Message ${e.message}.", e)

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
