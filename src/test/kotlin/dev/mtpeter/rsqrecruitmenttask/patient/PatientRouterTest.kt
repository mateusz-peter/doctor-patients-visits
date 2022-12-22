package dev.mtpeter.rsqrecruitmenttask.patient

import dev.mtpeter.rsqrecruitmenttask.configuration.RestResponsePage
import dev.mtpeter.rsqrecruitmenttask.configuration.TenantAwareRouting
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.take
import io.kotest.property.arbs.firstName
import io.kotest.property.arbs.geo.country
import io.kotest.property.arbs.lastName
import io.mockk.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse

class PatientRouterTest() : BehaviorSpec() {

    private val patientRepository: PatientRepository = mockk()
    private val patientHandler = PatientHandler(patientRepository)
    private val patientRouter = PatientRouter()
    private val tenantAwareRouting: TenantAwareRouting = mockk()
    private val webTestClient = WebTestClient
        .bindToRouterFunction(patientRouter.router(patientHandler, tenantAwareRouting)).build()

    val patientArb = arbitrary {
        val firstName = Arb.firstName().bind().toString()
        val lastName = Arb.lastName().bind().toString()
        val address = Arb.country().bind().toString()
        Patient(null, firstName, lastName, address)
    }

    init {

        coEvery { tenantAwareRouting.tenantAwareFilter(any(), any()) } coAnswers  { call ->
            val r = firstArg<ServerRequest>()
            val f = secondArg<suspend (ServerRequest) -> ServerResponse>()
            f(r)
        }

        given("GET Request on /patients/paged") {
            and("11 random patients") {
                val patients = patientArb.take(11)
                    .mapIndexed { index, patient -> patient.copy(id = index.toLong()) }
                    .toList()
                val patientsSortedByLastName = patients
                    .sortedWith(comparator = compareBy<Patient> {it.lastName}
                        .then(comparator = compareBy { it.firstName }))
                val sort = Sort.by("lastName").and(Sort.by("firstName"))

                coEvery { patientRepository.count() } returns patients.size.toLong()
                coEvery { patientRepository.findAllBy(any()) } answers {
                    val pr = firstArg<PageRequest>()
                    patientsSortedByLastName.drop(pr.pageNumber*pr.pageSize).take(pr.pageSize).asFlow()
                }

                `when`("Default page number and size") {

                    then("First 10 elements; 2 total pages") {
                        val page = webTestClient.get().uri("/patients/paged")
                            .exchange()
                            .expectStatus().isOk
                            .expectBody<RestResponsePage<Patient>>()
                            .returnResult().responseBody!!
                        page.number.shouldBe(0)
                        page.totalPages.shouldBe(2)
                        page.content.shouldContainInOrder(patientsSortedByLastName.take(10))

                        coVerify(exactly = 1) { patientRepository.count() }
                        coVerify(exactly = 1) { patientRepository.findAllBy(PageRequest.of(0, 10, sort))}
                    }
                }
                `when`("2nd page of size 5") {
                    then("Elements 5..9") {
                        val page = webTestClient.get().uri("/patients/paged?page=1&size=5")
                            .exchange()
                            .expectStatus().isOk
                            .expectBody<RestResponsePage<Patient>>()
                            .returnResult().responseBody!!
                        page.number.shouldBe(1)
                        page.totalPages.shouldBe(3)
                        page.content.shouldContainInOrder(patientsSortedByLastName.drop(5).take(5))

                        coVerify(exactly = 1) { patientRepository.count() }
                        coVerify(exactly = 1) { patientRepository.findAllBy(PageRequest.of(1, 5, sort)) }
                    }
                }
            }

        }

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

        given("DELETE Request on /patients/{id}") {

            `when`("/patient/1 does exist") {
                coEvery { patientRepository.existsById(1) } returns true
                coEvery { patientRepository.deleteById(1) } just Runs
                then("Patient is deleted, got OK") {
                    webTestClient.delete().uri("/patients/1")
                        .exchange()
                        .expectStatus().isOk
                    coVerify(exactly = 1) { patientRepository.existsById(1) }
                    coVerify(exactly = 1) { patientRepository.deleteById(1) }
                }
            }
            `when`("/patient/1 doesn't exist") {
                coEvery { patientRepository.existsById(1) } returns false
                then("We get 404") {
                    webTestClient.delete().uri("/patients/1")
                        .exchange()
                        .expectStatus().isNotFound
                    coVerify(exactly = 1) { patientRepository.existsById(1) }
                    coVerify(inverse = true) { patientRepository.deleteById(any()) }
                }
            }
            `when`("/patient/Jan - invalid request") {
                then("BadRequest") {
                    webTestClient.delete().uri("/patients/Jan")
                        .exchange()
                        .expectStatus().isBadRequest
                    verify { patientRepository wasNot called }
                }
            }
        }
    }
}
