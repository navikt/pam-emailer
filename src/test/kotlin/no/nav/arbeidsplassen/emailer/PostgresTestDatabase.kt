package no.nav.arbeidsplassen.emailer

import org.junit.jupiter.api.BeforeAll
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
abstract class PostgresTestDatabase {
    companion object {
        val db: PostgreSQLContainer = PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
            .apply {
                withDatabaseName("testdb")
                withUsername("username")
                withPassword("password")
            }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { db.jdbcUrl }
            registry.add("spring.datasource.password") { db.password }
            registry.add("spring.datasource.username") { db.username }
        }

        @JvmStatic
        @BeforeAll
        internal fun setUp() {
            db.start()
        }
    }
}