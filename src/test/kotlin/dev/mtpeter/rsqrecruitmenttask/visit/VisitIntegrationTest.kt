package dev.mtpeter.rsqrecruitmenttask.visit

import dev.mtpeter.rsqrecruitmenttask.configuration.*
import dev.mtpeter.rsqrecruitmenttask.doctor.DoctorRepository
import dev.mtpeter.rsqrecruitmenttask.multitenancy.*
import dev.mtpeter.rsqrecruitmenttask.patient.PatientRepository
import dev.mtpeter.rsqrecruitmenttask.utilities.*
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.kotest.property.arbitrary.single
import io.kotest.property.arbitrary.take
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.LocalTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VisitIntegrationTest(
    private val visitRepository: VisitRepository,
    private val doctorRepository: DoctorRepository,
    private val patientRepository: PatientRepository,
    @Value("\${local.server.port}")
    private val port: Int
) : BehaviorSpec() {

    private val webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:$port")
        .defaultHeader("X-TenantID", TENANT_A).build()

    init {
        this.afterTest {
            withTenant(TENANT_A) {
                visitRepository.deleteAll()
                patientRepository.deleteAll()
                doctorRepository.deleteAll()
            }
        }

        this.given("1 doctor; 2 patients") {
            val doctor = withTenant(TENANT_A) { doctorRepository.save(doctorArb.single()) }
            val patients = withTenant(TENANT_A) { patientRepository.saveAll(patientArb.take(2).asFlow()).toList() }

            `when`("Saved 15 visits for each patient") {

                val allVisits = patients.flatMapIndexed { patientIndex, patient ->
                    (0 until 15).map {
                        async {
                            val visit = visitArb.single()
                                .copy(
                                    doctorId = doctor.id!!,
                                    patientId = patient.id!!,
                                    visitTime = LocalTime.of(it, patientIndex)
                                )
                            webTestClient.post().uri("/visits").bodyValue(visit)
                                .exchange()
                                .expectStatus().isCreated
                                .expectBody<Visit>()
                                .returnResult().responseBody!!
                        }
                    }
                }.awaitAll()

                val firstPatientVisits = allVisits.filter { it.patientId == patients.first().id!! }
                val sortingComparator =
                    compareByDescending<Visit> { it.visitDate } then compareByDescending { it.visitTime }
                and("GET visits for all patients") {
                    val page = webTestClient.get().uri("/visits/paged").exchange()
                        .expectStatus().isOk
                        .expectBody<RestResponsePage<Visit>>()
                        .returnResult().responseBody!!

                    then("Last 10 visits (chronologically) of all patients, of total 3 pages") {
                        page.totalPages shouldBe 3
                        page.size shouldBe 10
                        page.number shouldBe 0
                        page.content shouldContainInOrder allVisits.sortedWith(sortingComparator).take(10)
                    }
                }
                and("GET visits for first patient, page=1, size=2") {
                    val page =
                        webTestClient.get().uri("/visits/paged?id=${patients.first().id!!}&page=1&size=5").exchange()
                            .expectStatus().isOk
                            .expectBody<RestResponsePage<Visit>>()
                            .returnResult().responseBody!!

                    then("Visits 5..9 (from last) of first patient, of total 3 pages") {
                        page.totalPages shouldBe 3
                        page.size shouldBe 5
                        page.number shouldBe 1
                        page.content shouldContainInOrder firstPatientVisits.sortedWith(sortingComparator).drop(5)
                            .take(5)
                    }
                }
            }

        }
    }
}