package dev.mtpeter.rsqrecruitmenttask.multitenant

import io.kotest.core.spec.style.BehaviorSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MTIntegrationTest @Autowired constructor(
    @Value("\${local.server.port}")
    private val port: Int
) : BehaviorSpec() {

    val webTestClient = WebTestClient
        .bindToServer().baseUrl("http://localhost:${port}").build()

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
                    webTestClient.get().uri("/patients").header("X-TenantID", "tenantX")
                        .exchange().expectStatus().isBadRequest
                }
            }
        }
    }
}