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
import com.microsoft.kiota.NativeResponseHandler
import com.microsoft.kiota.ResponseHandlerOption
import no.nav.arbeidsplassen.emailer.sendmail.Email
import okhttp3.Response
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.temporal.ChronoUnit


@Service
class EmailServiceAzure(private val aadProperties: AzureADProperties) {
    companion object {
        private val LOG = LoggerFactory.getLogger(EmailServiceAzure::class.java)
        private val secureLogsMarker: Marker = MarkerFactory.getMarker("TEAM_LOGS")
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
            LOG.warn(secureLogsMarker, "Failed to send email with $id. Response code ${e.responseStatusCode}. Code: ${e.error.code}. Message ${e.message}.", e)

            throw SendMailException(message = "Failed to send email with $id", status = HttpStatus.valueOf(e.responseStatusCode))
        } catch (e: Exception) {
            LOG.error("Failed to send email with $id. Unknown exception.", e)

            throw SendMailException(message = "Failed to send email with $id", e = e)
        }
    }

    fun sendEmailWithExtendedLogging(email: Email, id: String) {
        val emailRequestBody = createEmailRequestBody(email)

        val nativeResponseHandler = NativeResponseHandler()

        try {
            // A custom response handler makes the SDK skip its own status check, so we must check it ourselves
            graphClient.users()
                .byUserId(aadProperties.userPrincipal)
                .sendMail()
                .post(emailRequestBody) {
                    it.options.add(ResponseHandlerOption().apply {
                        responseHandler = nativeResponseHandler
                    })
                }
        } catch (e: SendMailException) {
            throw e
        } catch (e: Exception) {
            LOG.error("Failed to send email with $id. Unknown exception.", e)

            throw SendMailException(message = "Failed to send email with $id", e = e)
        }

        val response = nativeResponseHandler.value as? Response
            ?: throw SendMailException(message = "Failed to send email with $id. No response received from Azure.")

        response.use {
            handleSendMailResponse(it, id, email.recipient)
        }
    }

    private fun handleSendMailResponse(response: Response, id: String, recipient: String) {
        val statusCode = response.code
        val traceInfo = "Email id: $id. Status: $statusCode. " +
                "request-id: ${response.header("request-id")}. " +
                "client-request-id: ${response.header("client-request-id")}. " +
                "Date: ${response.header("Date")}."
        val secureTraceInfo = "$traceInfo Recipient: $recipient."

        when {
            statusCode == HttpStatus.ACCEPTED.value() -> {
                LOG.info("Email accepted by Azure. $traceInfo")
                LOG.info(secureLogsMarker, "Email accepted by Azure. $secureTraceInfo")
            }
            response.isSuccessful -> {
                LOG.error("Email sent to Azure, but got unexpected status (expected 202). $traceInfo")
                LOG.error(secureLogsMarker, "Email sent to Azure, but got unexpected status (expected 202). $secureTraceInfo")
            }
            else -> {
                LOG.error("Failed to send email. $traceInfo")
                LOG.error(secureLogsMarker, "Failed to send email. $secureTraceInfo Response body: ${response.body?.string()}")

                throw SendMailException(
                    message = "Failed to send email with $id",
                    status = HttpStatus.resolve(statusCode) ?: HttpStatus.INTERNAL_SERVER_ERROR
                )
            }
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
