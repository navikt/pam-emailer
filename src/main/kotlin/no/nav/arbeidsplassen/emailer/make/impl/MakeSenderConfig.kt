package no.nav.arbeidsplassen.emailer.make.impl

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class MakeSenderConfig {
    @Bean
    fun restClientBuilder(): RestClient.Builder = RestClient.builder()
}
