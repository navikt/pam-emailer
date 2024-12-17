package no.nav.arbeidsplassen.emailer.sendmail

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ScheduledOutboxEmailDeleter(private val outboxEmailRepository: OutboxEmailRepository) {

    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "deleteSentEmails", lockAtLeastFor = "PT10S", lockAtMostFor = "PT5M")
    fun deleteSentEmails() {
        outboxEmailRepository.deleteEmailsOlderThanAnHour()
    }

}