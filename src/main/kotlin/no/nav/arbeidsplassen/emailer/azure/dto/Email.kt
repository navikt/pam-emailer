package no.nav.arbeidsplassen.emailer.azure.dto

data class Email(val message:Message, val saveToSentItems:Boolean=false)

data class Message(val subject: String, val body: Body, val toRecipients: List<Recipient> = listOf<Recipient>())

data class Body(val contentType: MailContentType = MailContentType.TEXT, val content: String)

data class Recipient(val emailAddress: Address)

data class Address(val address: String)


enum class MailContentType {
    TEXT,
    HTML
}
