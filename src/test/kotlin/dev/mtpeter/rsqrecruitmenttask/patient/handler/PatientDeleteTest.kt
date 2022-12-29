package dev.mtpeter.rsqrecruitmenttask.patient.handler

import dev.mtpeter.rsqrecruitmenttask.patient.PatientRepository
import dev.mtpeter.rsqrecruitmenttask.visit.VisitRepository
import io.kotest.core.spec.style.behaviorSpec
import io.mockk.*
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient

fun patientDeleteTest(
    patientRepository: PatientRepository,
    visitRepository: VisitRepository,
    webTestClient: WebTestClient
) = behaviorSpec {

    given("/patients/1 exists") {
        coEvery { patientRepository.existsById(1) } returns true

        and("has visits") {
            `when`("DELETE /patients/1") {
                and("no cascade=true") {
                    coEvery { visitRepository.existsByPatientId(1) } returns true

                    then("Conflict") {
                        webTestClient.delete().uri("/patients/1").exchange()
                            .expectStatus().isEqualTo(HttpStatus.CONFLICT)

                        coVerify(exactly = 1) { patientRepository.existsById(1) }
                        coVerify(exactly = 1) { visitRepository.existsByPatientId(1) }
                        coVerify(inverse = true) { patientRepository.deleteById(1) }
                    }
                }
                and("cascade=true") {
                    coEvery { visitRepository.removeByPatientId(1) } just runs
                    coEvery { patientRepository.deleteById(1) } just runs

                    then("Delete visits and patient; NoContent") {
                        webTestClient.delete().uri("/patients/1?cascade=true").exchange()
                            .expectStatus().isNoContent

                        coVerify(exactly = 1) { patientRepository.existsById(1) }
                        coVerify(exactly = 1) { visitRepository.removeByPatientId(1) }
                        coVerify(exactly = 1) { patientRepository.deleteById(1) }
                    }
                }
            }
        }
        and("has no visits") {
            coEvery { visitRepository.existsByPatientId(1) } returns false
            `when`("DELETE /patients/1") {
                coEvery { patientRepository.deleteById(1) } just Runs

                then("Delete patient; Response NoContent") {
                    webTestClient.delete().uri("/patients/1")
                        .exchange()
                        .expectStatus().isNoContent

                    coVerify(exactly = 1) { patientRepository.existsById(1) }
                    coVerify(exactly = 1) { visitRepository.existsByPatientId(1) }
                    coVerify(exactly = 1) { patientRepository.deleteById(1) }
                }
            }
        }
    }
    given("Anything") {
        `when`("DELETE /patients/Jan") {
            then("BadRequest; DB untouched") {
                webTestClient.delete().uri("/patients/Jan")
                    .exchange()
                    .expectStatus().isBadRequest

                verify { patientRepository wasNot called }
            }
        }
    }
}