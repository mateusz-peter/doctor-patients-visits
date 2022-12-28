package dev.mtpeter.rsqrecruitmenttask.multitenant

import dev.mtpeter.rsqrecruitmenttask.configuration.TENANT_A
import dev.mtpeter.rsqrecruitmenttask.configuration.TENANT_B
import dev.mtpeter.rsqrecruitmenttask.configuration.patientArb
import dev.mtpeter.rsqrecruitmenttask.configuration.withTenant
import dev.mtpeter.rsqrecruitmenttask.patient.Patient
import dev.mtpeter.rsqrecruitmenttask.patient.PatientRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.property.arbitrary.take
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MTIntegrationTest @Autowired constructor(
    private val patientRepository: PatientRepository,
    @Value("\${local.server.port}")
    private val port: Int
) : BehaviorSpec() {

    val webTestClient = WebTestClient
        .bindToServer().baseUrl("http://localhost:${port}").build()

    init {
        this.given("Empty database of tenant 'tenantA'") {
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
        this.given("2 random patients in tenantA DB, 3 random patients in tenantB DB") {
            afterTest {
                withTenant(TENANT_A) {
                    patientRepository.deleteAll()
                }
                withTenant(TENANT_B) {
                    patientRepository.deleteAll()
                }
            }

            val patientsA = withTenant(TENANT_A) {
                patientRepository.saveAll(patientArb.take(2).asFlow()).toList()
            }
            val patientsB = withTenant(TENANT_B) {
                patientRepository.saveAll(patientArb.take(3).asFlow()).toList()
            }

            `when`("Get Request on /patients") {
                and("Header with tenantA") {
                    then("Response with patiens of tenantA") {
                        webTestClient.get().uri("/patients").header("X-TenantID", TENANT_A)
                            .exchange()
                            .expectStatus().isOk
                            .expectBodyList<Patient>().hasSize(2)
                            .contains(*patientsA.toTypedArray())
                    }
                }
                and("Header with tenantB") {
                    then("Response with patients of tenantB") {
                        webTestClient.get().uri("/patients").header("X-TenantID", TENANT_B)
                            .exchange()
                            .expectStatus().isOk
                            .expectBodyList<Patient>().hasSize(3)
                            .contains(*patientsB.toTypedArray())
                    }
                }
            }
        }
    }
}