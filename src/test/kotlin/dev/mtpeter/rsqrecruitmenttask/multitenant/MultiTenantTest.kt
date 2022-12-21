package dev.mtpeter.rsqrecruitmenttask.multitenant

import dev.mtpeter.rsqrecruitmenttask.patient.PatientHandler
import dev.mtpeter.rsqrecruitmenttask.patient.PatientRouter
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.coEvery
import io.mockk.mockk
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.buildAndAwait

class MultiTenantTest : BehaviorSpec() {

    private val patientHandler: PatientHandler = mockk()
    private val patientRouter = PatientRouter()

    private val webTestClient = WebTestClient
        .bindToRouterFunction(patientRouter.router(patientHandler)).build()

    init {
        given("GET Request on /patients") {
            coEvery { patientHandler.getAllPatients(any()) } returns ServerResponse.ok().buildAndAwait()

            `when`("TenantId not provided") {
                then("We get a bad request") {
                    webTestClient.get().uri("/patients")
                        .exchange()
                        .expectStatus().isBadRequest
                }
            }
            `when`("TenantId is provided") {
                then("We get 200") {
                    webTestClient.get().uri("/patients")
                        .header("X-TenantID", "tenantA")
                        .exchange()
                        .expectStatus().isOk
                }
            }
        }
    }
}