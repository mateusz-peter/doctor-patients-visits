package dev.mtpeter.rsqrecruitmenttask.patient

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import org.springframework.test.web.reactive.server.WebTestClient

class PatientRouterTest() : BehaviorSpec() {

    private val patientRepository: PatientRepository = mockk()
    private val patientHandler = PatientHandler(patientRepository)
    private val patientRouter = PatientRouter()
    private val webTestClient = WebTestClient
        .bindToRouterFunction(patientRouter.router(patientHandler)).build()

    override fun extensions() = listOf(SpringExtension)
    override fun isolationMode() = IsolationMode.InstancePerTest

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

            `when`("GET Request on /patient/Jan") {
                then("Bad Request") {
                    webTestClient.get().uri("/patients/Jan").exchange()
                        .expectStatus().isBadRequest
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

        given("POST Request on /patients") {

            `when`("a new Patient in body") {
                val postBody = PatientDTO("Jan", "Nowak-Jeziorański", "Radio Wolna Europa")
                coEvery { patientRepository.save(postBody.toPatient()) } returns postBody.toPatient(1)

                then("We get Created with proper Location Header and saved object with its id") {

                    webTestClient.post().uri("/patients").bodyValue(postBody)
                        .exchange()
                        .expectStatus().isCreated
                        .expectHeader().location("/patients/1")
                        .expectBody(Patient::class.java)
                        .isEqualTo(postBody.toPatient(1))

                    coVerify(exactly = 1) { patientRepository.save(postBody.toPatient()) }
                }
            }

            `when`("a garbage in body") {
                val postBody = mapOf("firstName" to "Jan", "lastName" to "Kowalski")

                then("We get BadRequest and no object is saved") {
                    webTestClient.post().uri("/patients").bodyValue(postBody)
                        .exchange()
                        .expectStatus().isBadRequest
                    verify { patientRepository wasNot called }
                }
            }
        }
    }
}
