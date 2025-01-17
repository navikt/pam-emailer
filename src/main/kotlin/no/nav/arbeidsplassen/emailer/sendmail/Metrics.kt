package no.nav.arbeidsplassen.emailer.sendmail

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.MultiGauge.Row
import io.micrometer.core.instrument.Tags
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service


@Service
class Metrics(private val meterRegistry: MeterRegistry, private val outboxEmailRepository: OutboxEmailRepository) {
    fun failedToSendEmail() {
        Counter.builder("emailer.emails_sent")
            .tags("result", "failure")
            .register(meterRegistry)
            .increment()
    }

    private val currentEmailCounts = MultiGauge.builder("emailer.current_email_counts")
        .register(meterRegistry);

    @Scheduled(fixedRate = 60_000)
    fun collectEmailStatuses() {
        val emailCounts = outboxEmailRepository.getEmailCounts()

        currentEmailCounts.register(
            listOf(
                Row.of(Tags.of("type", "sent_last_hour"), emailCounts.sentLastHour.toDouble()),
                Row.of(Tags.of("type", "pending"), emailCounts.pending.toDouble()),
                Row.of(Tags.of("type", "failed"), emailCounts.failed.toDouble())
            ),
            true
        )
    }
}