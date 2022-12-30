package dev.mtpeter.rsqrecruitmenttask.patient.handler

import dev.mtpeter.rsqrecruitmenttask.multitenancy.TenantAwareRouting
import dev.mtpeter.rsqrecruitmenttask.utilities.TenantAwareRoutingMock
import dev.mtpeter.rsqrecruitmenttask.patient.PatientRepository
import dev.mtpeter.rsqrecruitmenttask.patient.router.PatientHandler
import dev.mtpeter.rsqrecruitmenttask.patient.router.PatientRouter
import dev.mtpeter.rsqrecruitmenttask.patient.router.PatientService
import dev.mtpeter.rsqrecruitmenttask.visit.VisitRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.mockk
import org.springframework.test.web.reactive.server.WebTestClient

class PatientRouterTest() : BehaviorSpec() {

    private val patientRepository: PatientRepository = mockk()
    private val visitRepository: VisitRepository = mockk()
    private val patientService = PatientService(patientRepository, visitRepository)
    private val patientHandler = PatientHandler(patientService)
    private val patientRouter = PatientRouter()
    private val tenantAwareRouting: TenantAwareRouting = TenantAwareRoutingMock()
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
