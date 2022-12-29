package dev.mtpeter.rsqrecruitmenttask.doctor.handler

import dev.mtpeter.rsqrecruitmenttask.configuration.TenantAwareRouting
import dev.mtpeter.rsqrecruitmenttask.configuration.TenantAwareRoutingDummy
import dev.mtpeter.rsqrecruitmenttask.doctor.DoctorRepository
import dev.mtpeter.rsqrecruitmenttask.doctor.router.DoctorHandler
import dev.mtpeter.rsqrecruitmenttask.doctor.router.DoctorRouter
import dev.mtpeter.rsqrecruitmenttask.doctor.router.DoctorService
import dev.mtpeter.rsqrecruitmenttask.visit.VisitRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.mockk
import org.springframework.test.web.reactive.server.WebTestClient

class DoctorRouterTest : BehaviorSpec() {

    private val doctorRepository: DoctorRepository = mockk()
    private val visitRepository: VisitRepository = mockk()
    private val tenantAwareRouting: TenantAwareRouting = TenantAwareRoutingDummy()
    private val doctorService = DoctorService(doctorRepository, visitRepository)
    private val doctorHandler = DoctorHandler(doctorService)
    private val doctorRouter = DoctorRouter()
    private val webTestClient= WebTestClient.bindToRouterFunction(doctorRouter.routeDoctors(doctorHandler, tenantAwareRouting)).build()

    init {
        include(doctorGetAllTest(doctorRepository, webTestClient))
        include(doctorGetByIdTest(doctorRepository, webTestClient))
        include(doctorGetPagedTest(doctorRepository, webTestClient))

        include(doctorPostTest(doctorRepository, webTestClient))
        include(doctorPutTest(doctorRepository, webTestClient))
        include(doctorDeleteTest(doctorRepository, visitRepository, webTestClient))
    }
}
