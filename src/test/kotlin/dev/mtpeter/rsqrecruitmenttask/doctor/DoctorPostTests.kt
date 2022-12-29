package dev.mtpeter.rsqrecruitmenttask.doctor

import dev.mtpeter.rsqrecruitmenttask.configuration.doctorArb
import io.kotest.core.spec.style.behaviorSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.single
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.verify
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

fun doctorPostTest(doctorRepository: DoctorRepository, webTestClient: WebTestClient) = behaviorSpec {

    given("No preconditions") {
        `when`("POST Request on /doctors") {
            val validBody = doctorArb.single().toDTO()
            and("Valid body") {
                val savedId = Arb.long(10L..20L).single()
                coEvery { doctorRepository.save(validBody.toDoctor()) } returns validBody.toDoctor(savedId)

                then("Created and return saved doctor") {
                    webTestClient.post().uri("/doctors").bodyValue(validBody).exchange()
                        .expectStatus().isCreated
                        .expectHeader().location("/doctors/$savedId")
                        .expectBody<Doctor>().isEqualTo(validBody.toDoctor(savedId))

                    coVerify(exactly = 1) { doctorRepository.save(validBody.toDoctor()) }
                }
            }
            and("Invalid body") {
                val invalidBody = mapOf("firstName" to validBody.firstName, "lastName" to validBody.lastName)
                then("BadRequest and DB untouched") {
                    webTestClient.post().uri("/doctors").bodyValue(invalidBody).exchange()
                        .expectStatus().isBadRequest

                    verify { doctorRepository wasNot called }
                }
            }
            and("No body") {
                then("BadRequest and DB untouched") {
                    webTestClient.post().uri("/doctors").exchange()
                        .expectStatus().isBadRequest

                    verify { doctorRepository wasNot called }
                }
            }
        }
    }
}