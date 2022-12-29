package dev.mtpeter.rsqrecruitmenttask.doctor.handler

import dev.mtpeter.rsqrecruitmenttask.configuration.RestResponsePage
import dev.mtpeter.rsqrecruitmenttask.configuration.doctorArb
import dev.mtpeter.rsqrecruitmenttask.doctor.Doctor
import dev.mtpeter.rsqrecruitmenttask.doctor.DoctorRepository
import io.kotest.core.spec.style.behaviorSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.single
import io.kotest.property.arbitrary.take
import io.kotest.property.arbs.firstName
import io.mockk.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList

fun doctorGetAllTest(doctorRepository: DoctorRepository, webTestClient: WebTestClient) = behaviorSpec {

    given("No doctors in DB") {
        every { doctorRepository.findAll() } returns emptyFlow()

        `when`("GET /doctors") {
            then("Ok; empty list in body") {
                webTestClient.get().uri("/doctors").exchange()
                    .expectStatus().isOk
                    .expectBodyList<Doctor>().hasSize(0)

                verify(exactly = 1) { doctorRepository.findAll() }
            }
        }
    }
    given("10..20 random doctors in DB") {
        val docCount = Arb.int(10..20).single()
        val docs = doctorArb.take(docCount).mapIndexed {i,d -> d.copy(id = i.toLong())}.toList()
        every { doctorRepository.findAll() } returns docs.asFlow()

        `when`("GET /doctors") {
            then("Ok; 10 doctors in body") {
               webTestClient.get().uri("/doctors").exchange()
                   .expectStatus().isOk
                   .expectBodyList<Doctor>().hasSize(docCount)
                   .contains(*docs.toTypedArray())

                verify(exactly = 1) { doctorRepository.findAll() }
            }
        }
    }
}

fun doctorGetByIdTest(doctorRepository: DoctorRepository, webTestClient: WebTestClient) = behaviorSpec {

    val validId = Arb.long(10L..20L).single()
    given("A doctor of given id in DB") {
        val doctor = doctorArb.single().copy(id = validId)
        coEvery { doctorRepository.findById(validId) } returns doctor
        `when`("GET /doctors/{id}") {
            then("Ok; doctor in body") {
                webTestClient.get().uri("/doctors/$validId").exchange()
                    .expectStatus().isOk
                    .expectBody<Doctor>().isEqualTo(doctor)

                coVerify(exactly = 1) { doctorRepository.findById(validId) }
            }
        }
    }
    given("No doctor of given id in DB") {
        coEvery { doctorRepository.findById(validId) } returns null
        `when`("GET /doctors/{id}") {
            and("id is valid") {
                then("NotFound") {
                    webTestClient.get().uri("/doctors/$validId").exchange()
                        .expectStatus().isNotFound

                    coVerify(exactly = 1) { doctorRepository.findById(validId) }
                }
            }
            and("id is invalid") {
                val invalidId = Arb.firstName().single().name
                then("BadRequest") {
                    webTestClient.get().uri("/doctors/$invalidId").exchange()
                        .expectStatus().isBadRequest

                    verify { doctorRepository wasNot called }
                }
            }
        }
    }
}

fun doctorGetPagedTest(doctorRepository: DoctorRepository, webTestClient: WebTestClient) = behaviorSpec {

    given("11 random doctors") {
        val doctors = doctorArb.take(11).mapIndexed { i, d -> d.copy(id = i.toLong()) }.toList()
        val expectedSort = Sort.by("lastName").and(Sort.by("firstName"))
        coEvery { doctorRepository.count() } returns doctors.size.toLong()

        `when`("GET /doctors/paged") {
            and("Default page") {
                coEvery { doctorRepository.findAllBy(PageRequest.of(0, 10, expectedSort)) } returns doctors.take(10).asFlow()

                then("Ok; Page of first 10 doctors") {
                    val page = webTestClient.get().uri("/doctors/paged").exchange()
                        .expectStatus().isOk
                        .expectBody<RestResponsePage<Doctor>>()
                        .returnResult().responseBody!!
                    page.number.shouldBe(0)
                    page.content.shouldContainInOrder(doctors.take(10))
                    page.totalPages.shouldBe(2)

                    coVerify(exactly = 1) { doctorRepository.findAllBy(PageRequest.of(0, 10, expectedSort)) }
                    coVerify(exactly = 1) { doctorRepository.count() }
                }
            }
            and("page=1, size=5") {
                coEvery { doctorRepository.findAllBy(PageRequest.of(1, 5, expectedSort)) } returns doctors.drop(5).take(5).asFlow()

                then("Ok; Page of doctors 5..9") {
                    val page = webTestClient.get().uri("/doctors/paged?page=1&size=5").exchange()
                        .expectStatus().isOk
                        .expectBody<RestResponsePage<Doctor>>()
                        .returnResult().responseBody!!
                    page.number.shouldBe(1)
                    page.content.shouldContainInOrder(doctors.drop(5).take(5))
                    page.totalPages.shouldBe(3)

                    coVerify(exactly = 1) { doctorRepository.findAllBy(PageRequest.of(1, 5, expectedSort)) }
                    coVerify(exactly = 1) { doctorRepository.count() }
                }
            }
        }
    }
}