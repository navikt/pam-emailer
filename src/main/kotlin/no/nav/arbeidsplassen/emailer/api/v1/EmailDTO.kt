package no.nav.arbeidsplassen.emailer.api.v1

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class EmailDTO(
    val identifier: String?,
    val recipient: String,
    val subject: String,
    val content: String,
    val type: String,
    val attachments: List<AttachmentDto> = listOf()
)

data class AttachmentDto(
    val name: String,
    val contentType: String,
    val base64Content: String
)