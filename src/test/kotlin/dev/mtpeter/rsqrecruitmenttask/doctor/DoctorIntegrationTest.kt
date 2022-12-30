package dev.mtpeter.rsqrecruitmenttask.doctor

import dev.mtpeter.rsqrecruitmenttask.utilities.RestResponsePage
import dev.mtpeter.rsqrecruitmenttask.configuration.TENANT_A
import dev.mtpeter.rsqrecruitmenttask.utilities.doctorArb
import dev.mtpeter.rsqrecruitmenttask.utilities.withTenant
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldBeSortedWith
import io.kotest.matchers.shouldBe
import io.kotest.property.arbitrary.take
import kotlinx.coroutines.flow.toList
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DoctorIntegrationTest(
    private val doctorRepository: DoctorRepository,
    @Value("\${local.server.port}")
    private val port: Int
    ) : BehaviorSpec() {

    val webTestClient = WebTestClient
        .bindToServer().baseUrl("http://localhost:${port}")
        .defaultHeader("X-TenantID", TENANT_A).build()

    init {
        this.afterTest {
            withTenant(TENANT_A) {
                doctorRepository.deleteAll()
            }
        }

        this.given("11 Random doctors") {
            val doctorsToSave = doctorArb.take(11).toList()
            val doctors = withTenant(TENANT_A) {
                doctorRepository.saveAll(doctorsToSave).toList().sortedByDescending { it.lastName }
            }

            `when`("GET Request on /doctors/paged") {
                val sortingComparator = compareBy<Doctor> { it.lastName } then compareBy { it.firstName }
                and("Default page") {
                    then("10 doctors sorted by lastName and FirstName") {
                        val returnedDoctors = webTestClient.get().uri("/doctors/paged")
                            .exchange()
                            .expectStatus().isOk
                            .expectBody<RestResponsePage<Doctor>>()
                            .returnResult().responseBody!!

                        returnedDoctors.totalPages shouldBe 2
                        returnedDoctors.size shouldBe 10
                        returnedDoctors.forEach { it shouldBeIn doctors }
                        returnedDoctors shouldBeSortedWith sortingComparator
                    }
                }
                and("page=1, size=5") {
                    then("doctors 5..9 of sorted by lastName,firstName") {
                        val returnedDoctors = webTestClient.get().uri("/doctors/paged?page=1&size=5")
                            .exchange()
                            .expectStatus().isOk
                            .expectBody<RestResponsePage<Doctor>>()
                            .returnResult().responseBody!!

                        returnedDoctors.totalPages shouldBe 3
                        returnedDoctors.size shouldBe 5
                        returnedDoctors.content shouldBe doctors.sortedWith(sortingComparator).subList(5, 10)
                    }
                }
            }
        }
    }
}