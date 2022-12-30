package dev.mtpeter.rsqrecruitmenttask.patient.handler

import dev.mtpeter.rsqrecruitmenttask.utilities.RestResponsePage
import dev.mtpeter.rsqrecruitmenttask.utilities.patientArb
import dev.mtpeter.rsqrecruitmenttask.patient.Patient
import dev.mtpeter.rsqrecruitmenttask.patient.PatientRepository
import io.kotest.core.spec.style.behaviorSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.kotest.property.arbitrary.take
import io.mockk.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

fun patientsPagedGetTest(patientRepository: PatientRepository, webTestClient: WebTestClient) = behaviorSpec {
    given("11 random patients") {
        val patients = patientArb.take(11)
            .mapIndexed { index, patient -> patient.copy(id = index.toLong()) }
            .toList()
        val sort = Sort.by("lastName").and(Sort.by("firstName"))

        coEvery { patientRepository.count() } returns patients.size.toLong()
        coEvery { patientRepository.findAllBy(any()) } answers {
            val pr = firstArg<PageRequest>()
            patients.drop(pr.pageNumber * pr.pageSize).take(pr.pageSize).asFlow()
        }

        `when`("GET Request on /patients/paged") {
            and("Default page number and size") {
                val page = webTestClient.get().uri("/patients/paged")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody<RestResponsePage<Patient>>()
                    .returnResult().responseBody!!
                then("Response contains 10 first elements of 2 total pages") {
                    page.number.shouldBe(0)
                    page.totalPages.shouldBe(2)
                    page.content.shouldContainInOrder(patients.take(10))

                    coVerify(exactly = 1) { patientRepository.count() }
                    coVerify(exactly = 1) { patientRepository.findAllBy(PageRequest.of(0, 10, sort)) }
                }
            }
            and("page=1, size=5") {
                then("Response contains 5 first elements of 3 total pages") {
                    val page = webTestClient.get().uri("/patients/paged?page=1&size=5")
                        .exchange()
                        .expectStatus().isOk
                        .expectBody<RestResponsePage<Patient>>()
                        .returnResult().responseBody!!
                    page.number.shouldBe(1)
                    page.totalPages.shouldBe(3)
                    page.content.shouldContainInOrder(patients.drop(5).take(5))

                    coVerify(exactly = 1) { patientRepository.count() }
                    coVerify(exactly = 1) { patientRepository.findAllBy(PageRequest.of(1, 5, sort)) }
                }
            }
        }
    }
}

fun patientsGetByIdTest(patientRepository: PatientRepository, webTestClient: WebTestClient) = behaviorSpec {
    given("Patient at id=1 and no patient at id=2") {
        val examplePatient = Patient(1, "Jan", "Kowalski", "Poznań")
        coEvery { patientRepository.findById(1) } returns examplePatient
        coEvery { patientRepository.findById(2) } returns null

        `when`(" GET Request on /patients/1") {
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

        `when`("GET Request on /patients/Jan (invalid)") {
            then("Bad Request; DB untouched") {
                webTestClient.get().uri("/patients/Jan").exchange()
                    .expectStatus().isBadRequest

                verify { patientRepository wasNot called }
            }
        }
    }
}


fun patientsGetAllTest(patientRepository: PatientRepository, webTestClient: WebTestClient) = behaviorSpec {
    given("2 Patients at id=1 and id=2") {
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