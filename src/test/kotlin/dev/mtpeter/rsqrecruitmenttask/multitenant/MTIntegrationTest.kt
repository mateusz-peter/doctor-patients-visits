package dev.mtpeter.rsqrecruitmenttask.multitenant

import io.kotest.core.spec.style.BehaviorSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.MountableFile
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MTIntegrationTest @Autowired constructor(
    @Value("\${local.server.port}")
    private val port: Int
) : BehaviorSpec() {

    val webTestClient = WebTestClient
        .bindToServer().baseUrl("http://localhost:${port}").build()
        .mutate().responseTimeout(30.seconds.toJavaDuration()).build()

    companion object {
        val postgres = PostgreSQLContainer<Nothing>("postgres:15.1-alpine").apply {
            startupAttempts = 1
            withCopyToContainer(
                MountableFile.forClasspathResource("schema.sql"),
                "/docker-entrypoint-initdb.d/init.sql"
            )
            withUsername("test")
            withPassword("test")
            withDatabaseName("tenantA")
            start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("dev.mtpeter.rsq.tenants.tenantA.username", postgres::getUsername)
            registry.add("dev.mtpeter.rsq.tenants.tenantA.passowrd", postgres::getPassword)
            registry.add("dev.mtpeter.rsq.tenants.tenantA.url", ::r2dbcUrl)
        }

        fun r2dbcUrl() = "r2dbc:postgresql://${postgres.host}:${postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)}/${postgres.databaseName}"
    }


    init {
        this.given("GET Request on /patients") {
            `when`("No X-TenantID header provided") {
                then("We get BadRequest") {
                    webTestClient.get().uri("/patients").exchange().expectStatus().isBadRequest
                }
            }
            `when`("Valid tenant provided in X-TenantID header") {
                then("We get Ok") {
                    webTestClient.get().uri("/patients").header("X-TenantID", "tenantA")
                        .exchange().expectStatus().isOk
                }
            }
            `when`("Invalid tenant is provided in X-TenantID header") {
                then("We get BadRequest") {
                    webTestClient.get().uri("/patients").header("X-TenantID", "tenantB")
                        .exchange().expectStatus().isBadRequest
                }
            }
        }
    }
}