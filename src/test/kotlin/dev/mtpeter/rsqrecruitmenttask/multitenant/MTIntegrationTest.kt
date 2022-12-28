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
        this.given("Connection to empty database of tenant 'tenantA'") {
            `when`("GET Request on /patients") {
                and("Request doesn't include X-TenantID header") {
                    then("Response with BadRequest") {
                        webTestClient.get().uri("/patients").exchange().expectStatus().isBadRequest
                    }
                }
                and("Request include valid TenantId in X-TenantID header") {
                    then("Response with Ok") {
                        webTestClient.get().uri("/patients").header("X-TenantID", "tenantA")
                            .exchange().expectStatus().isOk
                    }
                }
                and("Request includes invalid TenantId in X-TenantID header") {
                    then("Response with BadRequest") {
                        webTestClient.get().uri("/patients").header("X-TenantID", "tenantX")
                            .exchange().expectStatus().isBadRequest
                    }
                }
            }
        }
    }
}