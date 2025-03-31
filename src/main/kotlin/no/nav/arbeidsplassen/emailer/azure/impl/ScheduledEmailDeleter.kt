package no.nav.arbeidsplassen.emailer.azure.impl

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ScheduledEmailDeleter(private val emailServiceAzure: EmailServiceAzure) {

    @Scheduled(cron = "0 0 1 * * *")
    @SchedulerLock(name = "deleteAzureAccountEmails", lockAtLeastFor = "PT10S", lockAtMostFor = "PT5M")
    fun deleteSentEmails() {
        emailServiceAzure.deleteAllEmailsInAccount()
    }

}