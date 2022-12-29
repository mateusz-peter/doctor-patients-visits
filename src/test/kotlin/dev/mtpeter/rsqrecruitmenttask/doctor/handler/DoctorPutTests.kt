package dev.mtpeter.rsqrecruitmenttask.doctor.handler

import dev.mtpeter.rsqrecruitmenttask.configuration.doctorArb
import dev.mtpeter.rsqrecruitmenttask.doctor.Doctor
import dev.mtpeter.rsqrecruitmenttask.doctor.DoctorRepository
import dev.mtpeter.rsqrecruitmenttask.doctor.toDTO
import io.kotest.core.spec.style.behaviorSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.single
import io.kotest.property.arbs.lastName
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.verify
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

fun doctorPutTest(doctorRepository: DoctorRepository, webTestClient: WebTestClient) = behaviorSpec {

    val validId = Arb.long(10L..20L).single()
    val validBody = doctorArb.single().toDTO()

    //creating with PUT would not be idempotent or make us use ids provided by client
    given("No doctor with given id") {
        coEvery { doctorRepository.existsById(validId) } returns false

        `when`("PUT /doctors/{id}") {
            and("id is invalid") {
                val invalidId = Arb.lastName().single().name

                then("BadRequest; DB untouched") {
                    webTestClient.put().uri("/doctors/$invalidId").bodyValue(validBody).exchange()
                        .expectStatus().isBadRequest

                    verify { doctorRepository wasNot called }
                }
            }
            and("id is valid") {
                then("NotFound") {
                    webTestClient.put().uri("/doctors/$validId").bodyValue(validBody).exchange()
                        .expectStatus().isNotFound

                    coVerify(exactly = 1) { doctorRepository.existsById(validId) }
                }
            }
        }
    }
    given("Doctor with given id exists") {
        coEvery { doctorRepository.existsById(validId) } returns true
        coEvery { doctorRepository.save(validBody.toDoctor(validId)) } returns validBody.toDoctor(validId)

        `when`("PUT /doctors/{id}") {
            and("body is invalid") {
                val invalidBody = mapOf("ashes" to "ashes")

                then("BadRequest; DB untouched") {
                    webTestClient.put().uri("/doctors/$validId").bodyValue(invalidBody).exchange()
                        .expectStatus().isBadRequest

                    verify { doctorRepository wasNot called }
                }
            }
            and("body is valid") {
                then("Ok; updated doctor in reply") {
                    webTestClient.put().uri("/doctors/$validId").bodyValue(validBody).exchange()
                        .expectStatus().isOk
                        .expectBody<Doctor>().isEqualTo(validBody.toDoctor(validId))

                    coVerify(exactly = 1) { doctorRepository.existsById(validId) }
                    coVerify(exactly = 1) { doctorRepository.save(validBody.toDoctor(validId)) }
                }
            }
        }
    }
}