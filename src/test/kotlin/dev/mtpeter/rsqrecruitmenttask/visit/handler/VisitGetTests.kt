package dev.mtpeter.rsqrecruitmenttask.visit.handler

import dev.mtpeter.rsqrecruitmenttask.utilities.RestResponsePage
import dev.mtpeter.rsqrecruitmenttask.utilities.visitArb
import dev.mtpeter.rsqrecruitmenttask.visit.Visit
import dev.mtpeter.rsqrecruitmenttask.visit.VisitRepository
import io.kotest.core.spec.style.behaviorSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.single
import io.kotest.property.arbitrary.take
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList

fun visitGetAllTest(visitRepository: VisitRepository, webTestClient: WebTestClient) = behaviorSpec {
    given("No visits") {
        every { visitRepository.findAll() } returns emptyFlow()
        `when`("GET /visits") {
            then("Ok; Empty List in body") {
                webTestClient.get().uri("/visits").exchange()
                    .expectStatus().isOk
                    .expectBodyList<Visit>().hasSize(0)

                verify(exactly = 1) { visitRepository.findAll() }
            }
        }
    }
    given("10 random visits") {
        val visits = visitArb.take(10).mapIndexed { i, v -> v.copy(id = i.toLong()) }.toList()
        every { visitRepository.findAll() } returns visits.asFlow()
        `when`("GET /visits") {
            then("Ok; 10 visits in body") {
                webTestClient.get().uri("/visits").exchange()
                    .expectStatus().isOk
                    .expectBodyList<Visit>().hasSize(10)
                    .contains(*visits.toTypedArray())

                verify(exactly = 1) { visitRepository.findAll() }
            }
        }
    }
}

fun visitGetPagedTest(visitRepository: VisitRepository, webTestClient: WebTestClient) = behaviorSpec {

    val defaultPage = PageRequest.of(0, 10, Sort.by("visitDate", "visitTime").descending())
    val customPage = PageRequest.of(1, 5, Sort.by("visitDate", "visitTime").descending())

    given("1 patient, 30 random visits, half of them of that patient") {
        val patientId = Arb.long(1000L..2000L).single()
        val visits = visitArb.take(30).mapIndexed { i, v ->
            v.copy(id = i.toLong(), patientId = if (i % 2 == 0) patientId else v.patientId)
        }.toList()


        `when`("GET Request on /visits/paged") {
            and("No patientId provided") {
                coEvery { visitRepository.count() } returns visits.size.toLong()

                and("Default page") {
                    every { visitRepository.findBy(defaultPage) } returns visits.take(10).asFlow()

                    then("10 first visits") {
                        val page = webTestClient.get().uri("/visits/paged").exchange()
                            .expectStatus().isOk
                            .expectBody<RestResponsePage<Visit>>()
                            .returnResult().responseBody!!
                        page.number shouldBe 0
                        page shouldContainInOrder visits.take(10)
                        page.totalPages shouldBe visits.size / 10

                        verify(exactly = 1) { visitRepository.findBy(defaultPage) }
                        coVerify(exactly = 1) { visitRepository.count() }
                    }
                }
                and("page=1, size=5") {
                    every { visitRepository.findBy(customPage) } returns visits.drop(5).take(5).asFlow()

                    then("Visits 5..9") {
                        val page = webTestClient.get().uri("/visits/paged?page=1&size=5").exchange()
                            .expectStatus().isOk
                            .expectBody<RestResponsePage<Visit>>()
                            .returnResult().responseBody!!
                        page.number shouldBe 1
                        page shouldContainInOrder visits.drop(5).take(5)
                        page.totalPages shouldBe visits.size / 5

                        verify(exactly = 1) { visitRepository.findBy(customPage) }
                        coVerify(exactly = 1) { visitRepository.count() }
                    }
                }
            }
            and("Provided patientId") {
                val patientVisits = visits.filter { it.patientId == patientId }
                coEvery { visitRepository.countByPatientId(patientId) } returns patientVisits.size.toLong()

                and("Default page") {
                    every { visitRepository.findByPatientId(patientId, defaultPage) } returns patientVisits.take(10)
                        .asFlow()

                    then("First 10 visits") {
                        val page = webTestClient.get().uri("/visits/paged?id=$patientId").exchange()
                            .expectStatus().isOk
                            .expectBody<RestResponsePage<Visit>>()
                            .returnResult().responseBody!!
                        page.number.shouldBe(0)
                        page.content.shouldContainInOrder(patientVisits.take(10))
                        page.totalPages.shouldBe((patientVisits.size + 9) / 10)

                        verify(exactly = 1) { visitRepository.findByPatientId(patientId, defaultPage) }
                        coVerify(exactly = 1) { visitRepository.countByPatientId(patientId) }
                    }
                }
                and("page=1, size=5") {
                    every { visitRepository.findByPatientId(patientId, customPage) } returns patientVisits.drop(5)
                        .take(5).asFlow()

                    then("Visits 5..9") {
                        val page = webTestClient.get().uri("/visits/paged?id=$patientId&page=1&size=5").exchange()
                            .expectStatus().isOk
                            .expectBody<RestResponsePage<Visit>>()
                            .returnResult().responseBody!!
                        page.number.shouldBe(1)
                        page.content.shouldContainInOrder(patientVisits.drop(5).take(5))
                        page.totalPages.shouldBe((patientVisits.size + 4) / 5)

                        verify(exactly = 1) { visitRepository.findByPatientId(patientId, customPage) }
                        coVerify(exactly = 1) { visitRepository.countByPatientId(patientId) }
                    }
                }
            }
        }
    }
}