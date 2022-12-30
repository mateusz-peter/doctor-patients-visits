package dev.mtpeter.rsqrecruitmenttask.doctor.handler

import dev.mtpeter.rsqrecruitmenttask.utilities.doctorArb
import dev.mtpeter.rsqrecruitmenttask.doctor.Doctor
import dev.mtpeter.rsqrecruitmenttask.doctor.DoctorRepository
import dev.mtpeter.rsqrecruitmenttask.visit.VisitRepository
import io.kotest.core.spec.style.behaviorSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.single
import io.kotest.property.arbs.color
import io.mockk.*
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

fun doctorDeleteTest(doctorRepository: DoctorRepository, visitRepository: VisitRepository, webTestClient: WebTestClient) = behaviorSpec {

    val validId = Arb.long(10L..20L).single()
    val docToDelete = doctorArb.single().copy(id = validId)

    given("Doctor with given id exists") {
        coEvery { doctorRepository.findById(validId) } returns docToDelete
        coEvery { doctorRepository.deleteById(validId) } just runs

        and("has visits") {
            coEvery { visitRepository.existsByDoctorId(validId) } returns true
            coEvery { visitRepository.removeByDoctorId(validId) } just runs

            `when`("DELETE /doctors/{id}") {
                and("cascade=true") {
                    then("Ok; Visits and doctor deleted") {
                        webTestClient.delete().uri("/doctors/$validId?cascade=true").exchange()
                            .expectStatus().isOk
                            .expectBody<Doctor>().isEqualTo(docToDelete)

                        coVerify(exactly = 1) { doctorRepository.findById(validId) }
                        coVerify(inverse = true) { visitRepository.existsByDoctorId(validId) }
                        coVerify(exactly = 1) { doctorRepository.deleteById(validId) }
                        coVerify(exactly = 1) { visitRepository.removeByDoctorId(validId) }
                    }
                }
                and("cascade=false (default)") {
                    then("Conflict") {
                        webTestClient.delete().uri("/doctors/$validId").exchange()
                            .expectStatus().isEqualTo(HttpStatus.CONFLICT)

                        coVerify(exactly = 1) { doctorRepository.findById(validId) }
                        coVerify(exactly = 1) { visitRepository.existsByDoctorId(validId) }
                        coVerify(inverse = true) { doctorRepository.deleteById(validId) }
                        coVerify(inverse = true) { visitRepository.removeByDoctorId(validId) }
                    }
                }
            }
        }
        and("has no visits") {
            coEvery { visitRepository.existsByDoctorId(validId) } returns false

            `when`("DELETE /doctors/{id}") {
                then("Ok; doctor deleted") {
                    webTestClient.delete().uri("/doctors/$validId").exchange()
                        .expectStatus().isOk
                        .expectBody<Doctor>().isEqualTo(docToDelete)

                    coVerify(exactly = 1) { doctorRepository.findById(validId) }
                    coVerify(exactly = 1) { visitRepository.existsByDoctorId(validId) }
                    coVerify(exactly = 1) { doctorRepository.deleteById(validId) }
                }
            }
        }
    }
    given("Doctor with given id doesn't exist") {
        coEvery { doctorRepository.findById(validId) } returns null

        `when`("DELETE /doctors/{id}") {
            and("Valid id") {
                then("NotFound") {
                    webTestClient.delete().uri("/doctors/$validId").exchange()
                        .expectStatus().isNotFound

                    coVerify(exactly = 1) { doctorRepository.findById(validId) }
                }
            }
            and("Invalid id") {
                val invalidId = Arb.color().single().value

                then("BadRequest") {
                    webTestClient.delete().uri("/doctors/$invalidId").exchange()
                        .expectStatus().isBadRequest

                    verify { doctorRepository wasNot called }
                }
            }
        }
    }
}