package dev.mtpeter.rsqrecruitmenttask.visit

import dev.mtpeter.rsqrecruitmenttask.configuration.RestResponsePage
import dev.mtpeter.rsqrecruitmenttask.configuration.TenantAwareRoutingDummy
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.arbs.geo.country
import io.mockk.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList

class VisitRouterTest : BehaviorSpec() {

    val visitRepository: VisitRepository = mockk()
    val tenantAwareRouting = TenantAwareRoutingDummy()
    val visitService = VisitService(visitRepository)
    val visitHandler = VisitHandler(visitService)
    val visitRouter = VisitRouter()

    val webTestClient = WebTestClient
        .bindToRouterFunction(visitRouter.routeVisits(visitHandler, tenantAwareRouting))
        .build()

    val visitArb = arbitrary {
        val visitDate = Arb.localDate().single()
        val visitTime = Arb.localTime().single().withSecond(0).withNano(0)
        val place = Arb.country().single().name
        val doctorId = Arb.long(10L..20L).single()
        val patientId = Arb.long(10L..20L).single()
        Visit(null, visitDate, visitTime, place, doctorId, patientId)
    }

    init {

        given("GET Request on /visits") {
            `when`("No visits") {
                every { visitRepository.findAll() } returns emptyFlow()
                then("Return empty list") {
                    webTestClient.get().uri("/visits").exchange()
                        .expectStatus().isOk
                        .expectBodyList<Visit>().hasSize(0)
                }
            }
            `when`("10 random visits") {
                val visits = visitArb.take(10).mapIndexed { i, v -> v.copy(id = i.toLong()) }.toList()
                every { visitRepository.findAll() } returns visits.asFlow()
                then("Return 10 visits") {
                    webTestClient.get().uri("/visits").exchange()
                        .expectStatus().isOk
                        .expectBodyList<Visit>().hasSize(10)
                }
            }
        }
        given("GET Request on /visits/paged") {
            val patientXId = Arb.long(1000L..2000L).single()
            val visits = visitArb.take(30).mapIndexed { i, v ->
                v.copy(id = i.toLong(), patientId = if (i % 2 == 0) patientXId else v.patientId)
            }.toList()
            val patientXVisits = visits.filter { it.patientId == patientXId }
            val defaultPage = PageRequest.of(0, 10, Sort.by("visitDate", "visitTime").descending())
            val customPage = PageRequest.of(1, 5, Sort.by("visitDate", "visitTime").descending())

            `when`("No patientId provided") {
                and("Default page") {
                    every { visitRepository.findBy(defaultPage) } returns visits.take(10).asFlow()
                    coEvery { visitRepository.count() } returns visits.size.toLong()
                    then("10 first visits") {
                        val page = webTestClient.get().uri("/visits/paged").exchange()
                            .expectStatus().isOk
                            .expectBody<RestResponsePage<Visit>>()
                            .returnResult().responseBody!!
                        page.number.shouldBe(0)
                        page.shouldContainInOrder(visits.take(10))
                        page.totalPages.shouldBe(visits.size / 10)

                        verify(exactly = 1) { visitRepository.findBy(defaultPage) }
                        coVerify(exactly = 1) { visitRepository.count() }
                    }
                }
                and("page #1 with 5 visits") {
                    every { visitRepository.findBy(customPage) } returns visits.drop(5).take(5).asFlow()
                    coEvery { visitRepository.count() } returns visits.size.toLong()
                    then("visits 5..9") {
                        val page = webTestClient.get().uri("/visits/paged?page=1&size=5").exchange()
                            .expectStatus().isOk
                            .expectBody<RestResponsePage<Visit>>()
                            .returnResult().responseBody!!
                        page.number.shouldBe(1)
                        page.shouldContainInOrder(visits.drop(5).take(5))
                        page.totalPages.shouldBe(visits.size / 5)

                        verify(exactly = 1) { visitRepository.findBy(customPage) }
                        coVerify(exactly = 1) { visitRepository.count() }
                    }
                }
            }
            `when`("patientId provided") {
                and("Default page") {
                    every { visitRepository.findByPatientId(patientXId, defaultPage) } returns patientXVisits.take(10)
                        .asFlow()
                    coEvery { visitRepository.countByPatientId(patientXId) } returns patientXVisits.size.toLong()
                    then("10 first visits") {
                        val page = webTestClient.get().uri("/visits/paged?id=$patientXId").exchange()
                            .expectStatus().isOk
                            .expectBody<RestResponsePage<Visit>>()
                            .returnResult().responseBody!!
                        page.number.shouldBe(0)
                        page.content.shouldContainInOrder(patientXVisits.take(10))
                        page.totalPages.shouldBe((patientXVisits.size + 9) / 10)

                        verify(exactly = 1) { visitRepository.findByPatientId(patientXId, defaultPage) }
                        coVerify(exactly = 1) { visitRepository.countByPatientId(patientXId) }
                    }
                }
                and("Custom Page") {
                    every { visitRepository.findByPatientId(patientXId, customPage) } returns patientXVisits.drop(5)
                        .take(5).asFlow()
                    coEvery { visitRepository.countByPatientId(patientXId) } returns patientXVisits.size.toLong()

                    then("visits 5..9") {
                        val page = webTestClient.get().uri("/visits/paged?id=$patientXId&page=1&size=5").exchange()
                            .expectStatus().isOk
                            .expectBody<RestResponsePage<Visit>>()
                            .returnResult().responseBody!!
                        page.number.shouldBe(1)
                        page.content.shouldContainInOrder(patientXVisits.drop(5).take(5))
                        page.totalPages.shouldBe((patientXVisits.size + 4) / 5)

                        verify(exactly = 1) { visitRepository.findByPatientId(patientXId, customPage) }
                        coVerify(exactly = 1) { visitRepository.countByPatientId(patientXId) }
                    }
                }
            }
        }
    }
}
