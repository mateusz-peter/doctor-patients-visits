package dev.mtpeter.rsqrecruitmenttask.visit.handler

import dev.mtpeter.rsqrecruitmenttask.utilities.visitArb
import dev.mtpeter.rsqrecruitmenttask.visit.Visit
import dev.mtpeter.rsqrecruitmenttask.visit.VisitRepository
import dev.mtpeter.rsqrecruitmenttask.visit.toDTO
import io.kotest.core.spec.style.behaviorSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.single
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.verify
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

fun visitRescheduleTests(visitRepository: VisitRepository, webTestClient: WebTestClient) = behaviorSpec {
    val validId = Arb.long(10L..20L).single()
    val validBody = visitArb.single().toDTO()

    given("Visit to reschedule exists") {
        coEvery { visitRepository.findById(validId) } returns validBody.toVisit(validId)

        and("Conflicting visit exists") {
            val conflictingVisit = visitArb.single()
                .copy(id = validId+1, visitDate = validBody.visitDate, visitTime = validBody.visitTime, doctorId = validBody.doctorId)
            coEvery { visitRepository.findByVisitDateAndVisitTimeAndDoctorId(validBody.visitDate, validBody.visitTime, validBody.doctorId) } returns conflictingVisit

            `when`("PUT request on /visits/{id}") {

                then("Conflict; visit is unchanged") {
                    webTestClient.put().uri("/visits/$validId").bodyValue(validBody).exchange()
                        .expectStatus().isEqualTo(HttpStatus.CONFLICT)

                    coVerify(exactly = 1) { visitRepository.findById(validId) }
                    coVerify(exactly = 1) { visitRepository.findByVisitDateAndVisitTimeAndDoctorId(validBody.visitDate, validBody.visitTime, validBody.doctorId) }
                    coVerify(inverse = true) { visitRepository.save(any()) }
                }
            }
        }
        and("Conflicting visit doesn't exist") {
            coEvery { visitRepository.findByVisitDateAndVisitTimeAndDoctorId(validBody.visitDate, validBody.visitTime, validBody.doctorId) } returns null
            coEvery { visitRepository.save(validBody.toVisit(validId)) } returns validBody.toVisit(validId)

            `when`("PUT Request on /visits/{id}") {
                then("Ok; return updated visit") {
                    webTestClient.put().uri("/visits/$validId").bodyValue(validBody).exchange()
                        .expectStatus().isOk
                        .expectBody<Visit>().isEqualTo(validBody.toVisit(validId))

                    coVerify(exactly = 1) { visitRepository.findById(validId) }
                    coVerify(exactly = 1) { visitRepository.findByVisitDateAndVisitTimeAndDoctorId(validBody.visitDate, validBody.visitTime, validBody.doctorId)}
                    coVerify(exactly = 1) { visitRepository.save(validBody.toVisit(validId))}
                }
            }
        }
        and("Conflicting visit doesn't matter") {
            and("Trying to change patientId") {
                val invalidBody = validBody.copy(patientId = validBody.patientId+1)
                coEvery { visitRepository.findById(validId) } returns validBody.toVisit(validId)

                then("BadRequest") {
                    webTestClient.put().uri("/visits/$validId").bodyValue(invalidBody)
                        .exchange()
                        .expectStatus().isBadRequest

                    coVerify(exactly = 1) { visitRepository.findById(validId) }
                }
            }
        }
    }
    given("Visit to reschedule doesn't exist") {
        coEvery { visitRepository.findById(validId) } returns null
        `when`("PUT Request on /visits/{id}") {
            then("NotFound") {
                webTestClient.put().uri("/visits/$validId").bodyValue(validBody)
                    .exchange()
                    .expectStatus().isNotFound

                coVerify(exactly = 1) { visitRepository.findById(validId) }
            }
        }
    }
    given("Visit's existence doesn't matter") {
        `when`("PUT Request on /visits/{id}") {
            and("garbage body") {
                val invalidBody = mapOf("ashes" to "ashes", "dust" to "dust")
                then("BadRequest") {
                    webTestClient.put().uri("/visits/$validId").bodyValue(invalidBody)
                        .exchange()
                        .expectStatus().isBadRequest

                    verify { visitRepository wasNot called }
                }
            }
            and("invalid hour") {
                val invalidBody = validBody.copy(visitTime = validBody.visitTime.plusSeconds(3))
                then("BadRequest") {
                    webTestClient.put().uri("/visits/$validId").bodyValue(invalidBody)
                        .exchange()
                        .expectStatus().isBadRequest

                    verify { visitRepository wasNot called }
                }
            }
        }
    }
}