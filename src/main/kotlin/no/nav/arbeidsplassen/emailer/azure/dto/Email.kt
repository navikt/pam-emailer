package no.nav.arbeidsplassen.emailer.azure.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

data class Email(val message:Message, val saveToSentItems:Boolean=false)

data class Message(val subject: String, val body: Body, val toRecipients: List<Recipient> = listOf<Recipient>(),
                   @JsonInclude(JsonInclude.Include.NON_EMPTY) val attachments: List<Attachment>)

data class Body(val contentType: MailContentType = MailContentType.TEXT, val content: String)

data class Recipient(val emailAddress: Address)

data class Address(val address: String)

data class Attachment(
    val name: String,
    val contentType: String,
    val contentBytes: String,
    @JsonProperty("@odata.type")
    val odataType: String = "#microsoft.graph.fileAttachment",
    )

enum class MailContentType {
    TEXT,
    HTML
}
