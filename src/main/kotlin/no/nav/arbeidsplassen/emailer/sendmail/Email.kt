package no.nav.arbeidsplassen.emailer.sendmail

data class Email(
    val recipient: String,
    val subject: String,
    val content: String,
    val type: String,
    val attachments: List<Attachment> = listOf()
)

data class Attachment(
    val name: String,
    val contentType: String,
    val content: String
)

enum class Priority(val value: Int) {
    HIGH(10),
    NORMAL(5);

    companion object {
        fun fromValue(value: Int) = entries.first { it.value == value }
    }
}