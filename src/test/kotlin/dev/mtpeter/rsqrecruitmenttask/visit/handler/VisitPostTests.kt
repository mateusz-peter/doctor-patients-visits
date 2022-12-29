package dev.mtpeter.rsqrecruitmenttask.visit.handler

import dev.mtpeter.rsqrecruitmenttask.configuration.visitArb
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

fun visitScheduleTests(visitRepository: VisitRepository, webTestClient: WebTestClient) = behaviorSpec {
    val validBody = visitArb.single().toDTO()
    val visitId = Arb.long(10L..20L).single()

    given("Conflicting visit doesn't exist") {
        `when`("POST Request on /visits with valid body") {
            coEvery { visitRepository.findByVisitDateAndVisitTimeAndDoctorId(
                validBody.visitDate,
                validBody.visitTime,
                validBody.doctorId
            ) } returns null
            coEvery { visitRepository.save(validBody.toVisit()) } returns validBody.toVisit(visitId)

            then("Ok and return saved visit") {
                webTestClient.post().uri("/visits").bodyValue(validBody).exchange()
                    .expectStatus().isCreated
                    .expectHeader().location("/visits/$visitId")
                    .expectBody<Visit>().isEqualTo(validBody.toVisit(visitId))

                coVerify(exactly = 1) {
                    visitRepository.findByVisitDateAndVisitTimeAndDoctorId(
                        validBody.visitDate,
                        validBody.visitTime,
                        validBody.doctorId
                    )
                }
                coVerify(exactly = 1) { visitRepository.save(validBody.toVisit()) }
            }
        }
    }
    given("Conflicting visit exists") {
        coEvery {
            visitRepository.findByVisitDateAndVisitTimeAndDoctorId(
                validBody.visitDate,
                validBody.visitTime,
                validBody.doctorId
            )
        } returns validBody.toVisit(visitId)

        `when`("POST Request on /visits with valid body") {
            then("Conflict; Nothing saved") {
                webTestClient.post().uri("/visits").bodyValue(validBody).exchange()
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT)

                coVerify(exactly = 1) {
                    visitRepository.findByVisitDateAndVisitTimeAndDoctorId(
                        validBody.visitDate,
                        validBody.visitTime,
                        validBody.doctorId
                    )
                }
                coVerify(inverse = true) { visitRepository.save(any()) }
            }
        }
    }
    given("Conflicting visits don't matter") {
        `when`("POST Request on /visits") {
            and("garbage body") {
                val invalidBody = mapOf("garbage" to "trash")

                then("BadRequest; DB untouched") {
                    webTestClient.post().uri("/visits").bodyValue(invalidBody).exchange()
                        .expectStatus().isBadRequest

                    verify { visitRepository wasNot called }
                }
            }
            and("Visit time has seconds") {
                val invalidBody = validBody.copy(visitTime = validBody.visitTime.plusSeconds(3))
                then("BadRequest; DB untouched") {
                    webTestClient.post().uri("/visits").bodyValue(invalidBody).exchange()
                        .expectStatus().isBadRequest

                    verify { visitRepository wasNot called }
                }
            }
        }
    }
}