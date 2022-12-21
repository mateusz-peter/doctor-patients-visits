package dev.mtpeter.rsqrecruitmenttask.patient

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

class PatientRouterTest() : BehaviorSpec() {

    private val patientRepository: PatientRepository = mockk()
    private val patientHandler = PatientHandler(patientRepository)
    private val patientRouter = PatientRouter()
    private val webTestClient = WebTestClient
        .bindToRouterFunction(patientRouter.router(patientHandler)).build()

    init {

        given("GET Request on /patients/{id}; Patient at id=1 and no patient at id=2") {

            val examplePatient = Patient(1, "Jan", "Kowalski", "Poznań")
            coEvery { patientRepository.findById(1) } returns examplePatient
            coEvery { patientRepository.findById(2) } returns null

            `when`("GET Request on /patients/1") {
                then("Request is successful and we get the patient; Had searched in DB") {
                    webTestClient.get().uri { it.path("/patients/{id}").build(1) }
                        .exchange()
                        .expectStatus().isOk
                        .expectBody(Patient::class.java)
                        .isEqualTo(examplePatient)

                    coVerify(exactly = 1) { patientRepository.findById(1) }
                }
            }

            `when`("GET Request on /patients/2") {
                then("Request is replied with NotFound; Had searched in DB") {
                    webTestClient.get().uri("/patients/2").exchange()
                        .expectStatus().isNotFound

                    coVerify(exactly = 1) { patientRepository.findById(2) }
                }
            }

            `when`("GET Request on /patients/Jan (Invalid request)") {
                then("Bad Request; Hadn't searched in DB") {
                    webTestClient.get().uri("/patients/Jan").exchange()
                        .expectStatus().isBadRequest

                    verify { patientRepository wasNot called }
                }
            }
        }

        given("GET Request on /patients; 2 Patients at id=1 and id=2") {
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

                then("Created with proper Location Header and saved object with its id") {
                    webTestClient.post().uri("/patients").bodyValue(postBody)
                        .exchange()
                        .expectStatus().isCreated
                        .expectHeader().location("/patients/1")
                        .expectBody(Patient::class.java)
                        .isEqualTo(postBody.toPatient(1))

                    coVerify(exactly = 1) { patientRepository.save(postBody.toPatient()) }
                }
            }

            `when`("an invalid body") {
                val postBody = mapOf("firstName" to "Jan", "lastName" to "Kowalski")

                then("We get BadRequest and no object is saved") {
                    webTestClient.post().uri("/patients").bodyValue(postBody)
                        .exchange()
                        .expectStatus().isBadRequest
                    verify { patientRepository wasNot called }
                }
            }
        }

        given("PUT Request on /patients/{id}") {

            val putPatientDTO = PatientDTO("Jan", "Kowalski", "Poznań")
            val invalidBody = mapOf("firstName" to "Jan", "lastName" to "Kowalski")

            `when`("/patient/1 exists; a Patient in body") {
                coEvery { patientRepository.existsById(1) } returns true
                coEvery { patientRepository.save(putPatientDTO.toPatient(1)) } returns putPatientDTO.toPatient(1)

                then("Patient 1 is updated and returned in response") {
                    webTestClient.put().uri("/patients/1").bodyValue(putPatientDTO)
                        .exchange()
                        .expectStatus().isOk
                        .expectBody<Patient>().isEqualTo(putPatientDTO.toPatient(1))

                    coVerify(exactly = 1) { patientRepository.existsById(1) }
                    coVerify(exactly = 1) { patientRepository.save(putPatientDTO.toPatient(1)) }
                }
            }
            `when`("patient/1 exists; Invalid body") {
                coEvery { patientRepository.existsById(1) } returns true

                then("Bad Request; DB untouched") {
                    webTestClient.put().uri("/patients/1").bodyValue(invalidBody)
                        .exchange()
                        .expectStatus().isBadRequest

                    verify { patientRepository wasNot called }
                }
            }

            //Creating with PUT in our case would be either not idempotent or force us to use IDs provided by user
            `when`("/patient/1 doesn't exist; a Patient in body") {
                coEvery { patientRepository.existsById(1) } returns false

                then("Patient 1 IS NOT created; 404; DB was searched") {
                    webTestClient.put().uri("/patients/1").bodyValue(putPatientDTO)
                        .exchange()
                        .expectStatus().isNotFound

                    coVerify(exactly = 1) { patientRepository.existsById(1) }
                }
            }

            `when`("/patients/Jan; a Patient in body") {
                then("Bad Request; DB untouched") {
                    webTestClient.put().uri("/patients/Jan").bodyValue(putPatientDTO)
                        .exchange()
                        .expectStatus().isBadRequest

                    verify { patientRepository wasNot called }
                }
            }
        }
    }
}
