package dev.mtpeter.rsqrecruitmenttask.visit

import dev.mtpeter.rsqrecruitmenttask.configuration.TenantAwareRoutingDummy
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.mockk
import org.springframework.test.web.reactive.server.WebTestClient

class VisitRouterTest : BehaviorSpec() {

    val visitRepository: VisitRepository = mockk()
    val tenantAwareRouting = TenantAwareRoutingDummy()
    val visitService = VisitService(visitRepository)
    val visitHandler = VisitHandler(visitService)
    val visitRouter = VisitRouter()

    val webTestClient = WebTestClient
        .bindToRouterFunction(visitRouter.routeVisits(visitHandler, tenantAwareRouting))
        .build()

    init {
        include(visitGetAllTest(visitRepository, webTestClient))
        include(visitGetPagedTest(visitRepository, webTestClient))

        include(visitScheduleTests(visitRepository, webTestClient))
        include(visitRescheduleTests(visitRepository, webTestClient))
    }
}
