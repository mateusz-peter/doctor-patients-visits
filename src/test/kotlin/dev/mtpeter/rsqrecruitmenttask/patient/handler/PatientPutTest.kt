package dev.mtpeter.rsqrecruitmenttask.patient.handler

import dev.mtpeter.rsqrecruitmenttask.patient.Patient
import dev.mtpeter.rsqrecruitmenttask.patient.PatientDTO
import dev.mtpeter.rsqrecruitmenttask.patient.PatientRepository
import io.kotest.core.spec.style.behaviorSpec
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.verify
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

fun patientsPutTest(patientRepository: PatientRepository, webTestClient: WebTestClient) = behaviorSpec {
    val putPatientDTO = PatientDTO("Jan", "Kowalski", "Poznań")

    given("/patient/1 exists") {
        `when`("PUT Request on /patient/1") {
            coEvery { patientRepository.existsById(1) } returns true
            and("Valid body in request") {
                coEvery { patientRepository.save(putPatientDTO.toPatient(1)) } returns putPatientDTO.toPatient(1)
                then("Ok with updated patient") {
                    webTestClient.put().uri("/patients/1").bodyValue(putPatientDTO)
                        .exchange()
                        .expectStatus().isOk
                        .expectBody<Patient>().isEqualTo(putPatientDTO.toPatient(1))

                    coVerify(exactly = 1) { patientRepository.existsById(1) }
                    coVerify(exactly = 1) { patientRepository.save(putPatientDTO.toPatient(1)) }
                }
            }
            and("Invalid body in request") {
                val invalidBody = mapOf("firstName" to "Jan", "lastName" to "Kowalski")
                then("BadRequest") {
                    webTestClient.put().uri("/patients/1").bodyValue(invalidBody)
                        .exchange()
                        .expectStatus().isBadRequest

                    verify { patientRepository wasNot called }
                }
            }
        }
    }
    //Creating with PUT in our case would be either not idempotent or force us to use IDs provided by user
    given("/patient/1 doesn't exist") {
        coEvery { patientRepository.existsById(1) } returns false

        `when`("PUT Request on /patient/1") {
            then("NotFound") {
                webTestClient.put().uri("/patients/1").bodyValue(putPatientDTO)
                    .exchange()
                    .expectStatus().isNotFound

                coVerify(exactly = 1) { patientRepository.existsById(1) }
            }
        }
    }
    given("Anything") {
        `when`("PUT Request on /patient/Jan") {
            then("BadRequest") {
                webTestClient.put().uri("/patients/Jan").bodyValue(putPatientDTO)
                    .exchange()
                    .expectStatus().isBadRequest

                verify { patientRepository wasNot called }
            }
        }
    }
}