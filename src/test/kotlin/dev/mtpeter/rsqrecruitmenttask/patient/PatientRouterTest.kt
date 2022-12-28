package dev.mtpeter.rsqrecruitmenttask.patient

import dev.mtpeter.rsqrecruitmenttask.configuration.TenantAwareRouting
import dev.mtpeter.rsqrecruitmenttask.configuration.TenantAwareRoutingDummy
import dev.mtpeter.rsqrecruitmenttask.visit.VisitRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.mockk
import org.springframework.test.web.reactive.server.WebTestClient

class PatientRouterTest() : BehaviorSpec() {

    private val patientRepository: PatientRepository = mockk()
    private val visitRepository: VisitRepository = mockk()
    private val patientHandler = PatientHandler(patientRepository, visitRepository)
    private val patientRouter = PatientRouter()
    private val tenantAwareRouting: TenantAwareRouting = TenantAwareRoutingDummy()
    private val webTestClient = WebTestClient
        .bindToRouterFunction(patientRouter.routePatients(patientHandler, tenantAwareRouting)).build()

    init {
        include(patientsGetAllTest(patientRepository, webTestClient))
        include(patientsGetByIdTest(patientRepository, webTestClient))
        include(patientsPagedGetTest(patientRepository, webTestClient))

        include(patientPostTest(patientRepository, webTestClient))
        include(patientsPutTest(patientRepository, webTestClient))
        include(patientDeleteTest(patientRepository, visitRepository, webTestClient))
    }
}
