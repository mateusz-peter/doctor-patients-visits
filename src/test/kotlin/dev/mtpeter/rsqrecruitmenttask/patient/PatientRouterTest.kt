package dev.mtpeter.rsqrecruitmenttask.patient

import io.kotest.core.extensions.Extension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.springframework.test.web.reactive.server.WebTestClient

class PatientRouterTest() : BehaviorSpec() {

    private val patientRepository: PatientRepository = mockk()
    private val patientHandler = PatientHandler(patientRepository)
    private val patientRouter = PatientRouter(patientHandler)
    private val webTestClient = WebTestClient.bindToRouterFunction(patientRouter.router()).build()

    override fun extensions(): List<Extension> = listOf(SpringExtension)

    init {

        given("Patient at id=1 and no patient at id=2") {

            val examplePatient = Patient(1, "Jan", "Kowalski", "Poznań")
            coEvery { patientRepository.findById(1) } returns examplePatient
            coEvery { patientRepository.findById(2) } returns null

            `when`("GET Request on /patient/1") {
                then("Request is successful and we get the patient") {
                    webTestClient.get().uri { it.path("/patients/{id}").build(1) }
                        .exchange()
                        .expectStatus().isOk
                        .expectBody(Patient::class.java)
                        .isEqualTo(examplePatient)
                }
            }
            `when`("GET Request on /patient/2") {
                then("Request is replied with NotFound") {
                    webTestClient.get().uri("/patients/2").exchange()
                        .expectStatus().isNotFound
                }
            }
        }

        given("Patients at id=1 and id=2") {
            val examplePatient1 = Patient(1, "Jan", "Kowalski", "Poznań")
            val examplePatient2 = Patient(2, "Jan", "Nowak", "Poznań")
            every { patientRepository.findAll() } returns flowOf(examplePatient1, examplePatient2)

            `when`("GET Request on /patients") {
                then("Request is successful and we get all patients") {
                    webTestClient.get().uri("/patients").exchange()
                        .expectStatus().isOk
                        .expectBodyList(Patient::class.java)
                        .contains(examplePatient1, examplePatient2)
                }
            }
        }
    }
}
