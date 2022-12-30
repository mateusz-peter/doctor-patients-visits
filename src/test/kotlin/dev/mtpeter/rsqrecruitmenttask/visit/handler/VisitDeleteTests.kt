package dev.mtpeter.rsqrecruitmenttask.visit.handler

import dev.mtpeter.rsqrecruitmenttask.visit.VisitRepository
import io.kotest.core.spec.style.behaviorSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.positiveLong
import io.kotest.property.arbitrary.single
import io.kotest.property.arbs.color
import io.mockk.*
import org.springframework.test.web.reactive.server.WebTestClient

fun visitDeleteTest(visitRepository: VisitRepository, webTestClient: WebTestClient) = behaviorSpec {
    val validId = Arb.positiveLong().single()

    given("Visit with given {id} exists") {
        coEvery { visitRepository.existsById(validId) } returns true

        `when`("DELETE /visits/{id}") {
            coEvery { visitRepository.deleteById(validId) } just runs

            then("No Content; delete visit") {
                webTestClient.delete().uri("/visits/$validId").exchange()
                    .expectStatus().isNoContent

                coVerify(exactly = 1) { visitRepository.existsById(validId) }
                coVerify(exactly = 1) { visitRepository.deleteById(validId) }
            }
        }
    }
    given("Visit with given {id} doesn't exist") {
        coEvery { visitRepository.existsById(validId) } returns false

        `when`("DELETE /visits/{id}") {
            and("id is valid") {
                then("NotFound") {
                    webTestClient.delete().uri("/visits/$validId").exchange()
                        .expectStatus().isNotFound

                    coVerify(exactly = 1) { visitRepository.existsById(validId) }
                }
            }
            and("id is invalid") {
                val invalidId = Arb.color().single().value
                then("BadRequest") {
                    webTestClient.delete().uri("/visits/$invalidId").exchange()
                        .expectStatus().isBadRequest

                    verify { visitRepository wasNot called }
                }
            }
        }
    }
}