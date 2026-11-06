package no.nav.arbeidsplassen.emailer.make.impl

import no.nav.arbeidsplassen.emailer.sendmail.Email
import no.nav.arbeidsplassen.emailer.sendmail.Priority
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.web.client.RestClient
import java.util.UUID

/**
 * Manual, opt-in test that sends a REAL email through MakeSender.
 *
 * It is intentionally excluded from the regular test suite (e.g. `./gradlew test`) and only
 * runs when the environment variable SEND_REAL_EMAIL=true is set.
 *
 * Run it with real credentials, e.g.:
 *
 *   SEND_REAL_EMAIL=true \
 *   MAKESENDER_BASE_URL=https://api.makesender.no \
 *   MAKESENDER_DOMAIN=test.arbeidsplassen.nav.no \
 *   MAKESENDER_USER_ID=... \
 *   MAKESENDER_API_KEY=... \
 *   MAKESENDER_SENDER=no-reply@test.arbeidsplassen.nav.no \
 *   MAKESENDER_RECIPIENT=you@example.com \
 *   ./gradlew cleanTest test --tests "*MakeSenderManualEmailIT"
 */
@EnabledIfEnvironmentVariable(named = "SEND_REAL_EMAIL", matches = "true")
class MakeSenderManualEmailIT {

    @Test
    fun `sends a real email through MakeSender`() {
        val domain = requireEnv("MAKESENDER_DOMAIN")
        val properties = MakeSenderProperties(
            baseUrl = env("MAKESENDER_BASE_URL", "https://api.makesender.no"),
            domain = domain,
            userId = requireEnv("MAKESENDER_USER_ID"),
            apiKey = requireEnv("MAKESENDER_API_KEY"),
            sender = env("MAKESENDER_SENDER", "no-reply@$domain"),
        )

        val recipient = requireEnv("MAKESENDER_RECIPIENT")
        val client = MakeSenderEmailClient(properties, RestClient.builder())

        val id = UUID.randomUUID().toString()
        val email = Email(
            recipient = recipient,
            subject = "pam-emailer manual test $id",
            content = "<p>This is a real email sent by MakeSenderManualEmailIT. Id: $id</p>",
            priority = Priority.NORMAL,
            type = "html",
        )

        client.sendEmail(email, id)

        println("Sent real email to $recipient with id $id")
    }

    private fun env(name: String, default: String): String =
        System.getenv(name)?.takeIf { it.isNotBlank() } ?: default

    private fun requireEnv(name: String): String =
        System.getenv(name)?.takeIf { it.isNotBlank() }
            ?: error("Environment variable $name must be set to send a real email")
}
