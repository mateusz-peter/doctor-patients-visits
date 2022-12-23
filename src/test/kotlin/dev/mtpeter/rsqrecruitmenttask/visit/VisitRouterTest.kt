 package dev.mtpeter.rsqrecruitmenttask.visit

import dev.mtpeter.rsqrecruitmenttask.configuration.TenantAwareRoutingDummy
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.arbs.geo.country
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import org.springframework.test.web.reactive.server.WebTestClient
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



    }
}
