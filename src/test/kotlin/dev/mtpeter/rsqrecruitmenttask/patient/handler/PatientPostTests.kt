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

fun patientPostTest(patientRepository: PatientRepository, webTestClient: WebTestClient) = behaviorSpec {
    given("Empty Database") {
        `when`("POST Request on /patients") {
            and("a new Patient in body") {
                val postBody = PatientDTO("Jan", "Nowak-Jeziorański", "Radio Wolna Europa")
                coEvery { patientRepository.save(postBody.toPatient()) } returns postBody.toPatient(1)

                then("Created with proper Location Header and saved object with its id") {
                    webTestClient.post().uri("/patients").bodyValue(postBody)
                        .exchange()
                        .expectStatus().isCreated
                        .expectHeader().location("/patients/1")
                        .expectBody<Patient>().isEqualTo(postBody.toPatient(1))

                    coVerify(exactly = 1) { patientRepository.save(postBody.toPatient()) }
                }
            }
            and("an invalid body") {
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