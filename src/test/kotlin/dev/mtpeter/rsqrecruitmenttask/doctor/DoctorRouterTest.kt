package dev.mtpeter.rsqrecruitmenttask.doctor

import dev.mtpeter.rsqrecruitmenttask.configuration.RestResponsePage
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.single
import io.kotest.property.arbitrary.take
import io.kotest.property.arbs.color
import io.kotest.property.arbs.firstName
import io.kotest.property.arbs.geo.continent
import io.kotest.property.arbs.lastName
import io.mockk.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList

class DoctorRouterTest : BehaviorSpec() {

    val doctorRepository: DoctorRepository = mockk()
    val doctorService = DoctorService(doctorRepository)
    val doctorHandler = DoctorHandler(doctorService)
    val doctorRouter = DoctorRouter()
    val webTestClient= WebTestClient.bindToRouterFunction(doctorRouter.routeDoctors(doctorHandler)).build()

    val doctorArb = arbitrary {
        val firstName = Arb.firstName().bind().name
        val lastName = Arb.lastName().bind().name
        val specialty = Arb.color().bind().value
        Doctor(null, firstName, lastName, specialty)
    }

    init {

        given("GET Request on /doctors") {
            `when`("No doctors in DB") {
                coEvery { doctorRepository.findAll() } returns emptyFlow()
                then("Return empty list after searching in DB") {
                    webTestClient.get().uri("/doctors").exchange()
                        .expectStatus().isOk
                        .expectBodyList<Doctor>().hasSize(0)
                    coVerify(exactly = 1) { doctorRepository.findAll() }
                }
            }
            `when`("10 random doctors in DB") {
                val docs = doctorArb.take(10)
                    .mapIndexed { index, doctor -> doctor.copy(id = index.toLong()) }.toList()
                coEvery { doctorRepository.findAll() } returns docs.asFlow()
                then("Return 10 doctors") {
                    webTestClient.get().uri("/doctors").exchange()
                        .expectStatus().isOk
                        .expectBodyList<Doctor>().hasSize(10)
                }
            }
        }
        given("GET Request on /doctors/{id}") {
            val validId = Arb.long(10L..20L).single()
            val invalidId = Arb.continent().single().toString()
            val doctor = doctorArb.single().copy(id = validId)
            `when`("Id is valid; Doctor is present") {
                coEvery { doctorRepository.findById(validId) } returns doctor
                then("Return the doctor") {
                    webTestClient.get().uri("/doctors/$validId").exchange()
                        .expectStatus().isOk
                        .expectBody<Doctor>().isEqualTo(doctor)
                    coVerify(exactly = 1) { doctorRepository.findById(validId) }
                }
            }
            `when`("Id is valid; No doctor at id") {
                coEvery { doctorRepository.findById(validId) } returns null
                then("Return NotFound") {
                    webTestClient.get().uri("/doctors/$validId").exchange()
                        .expectStatus().isNotFound
                    coVerify(exactly = 1) { doctorRepository.findById(validId) }
                }
            }
            `when`("Id is invalid") {
                then("Return BadRequest; Don't touch DB") {
                    webTestClient.get().uri("/doctors/$invalidId").exchange()
                        .expectStatus().isBadRequest
                    verify { doctorRepository wasNot called }
                }
            }
        }
        given("GET Request on /doctors/paged") {
            and("11 random doctors") {
                val doctors = doctorArb.take(11).mapIndexed { i, d -> d.copy(id = i.toLong()) }.toList()
                val expectedSort = Sort.by("lastName").and(Sort.by("firstName"))
                coEvery { doctorRepository.count() } returns doctors.size.toLong()

                `when`("Default request") {
                    coEvery { doctorRepository.findAllBy(PageRequest.of(0, 10, expectedSort)) } returns doctors.take(10).asFlow()
                    then("page 0, 10 doctors, total 2 pages") {
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
                `when`("Second page of size 5") {
                    coEvery { doctorRepository.findAllBy(PageRequest.of(1, 5, expectedSort)) } returns doctors.drop(5).take(5).asFlow()
                    then("page 1, doctors 5..9, total 3 pages") {
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
        given("POST Request on /doctors") {
            val validBody = doctorArb.single().toDTO()

            `when`("Valid body") {
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
            `when`("Invalid body") {
                val invalidBody = mapOf("firstName" to validBody.firstName, "lastName" to validBody.lastName)
                then("BadRequest and DB untouched") {
                    webTestClient.post().uri("/doctors").bodyValue(invalidBody).exchange()
                        .expectStatus().isBadRequest

                    verify { doctorRepository wasNot called }
                }
            }
            `when`("No body") {
                then("BadRequest and DB untouched") {
                    webTestClient.post().uri("/doctors").exchange()
                        .expectStatus().isBadRequest

                    verify { doctorRepository wasNot called }
                }
            }
        }
        given("PUT Request on /doctors") {
            val validBody = doctorArb.single().toDTO()
            `when`("Valid Id") {
                val validId = Arb.long(10L..20L).single()
                and("Valid Body") {
                    and("Doctor exists") {
                        coEvery { doctorRepository.existsById(validId) } returns true
                        coEvery { doctorRepository.save(validBody.toDoctor()) } returns validBody.toDoctor(validId)
                        then("Return updated entity with Ok") {
                            webTestClient.put().uri("/doctors/$validId").bodyValue(validBody).exchange()
                                .expectStatus().isOk
                                .expectBody<Doctor>().isEqualTo(validBody.toDoctor(validId))

                            coVerify(exactly = 1) { doctorRepository.existsById(validId) }
                            coVerify(exactly = 1) { doctorRepository.save(validBody.toDoctor()) }
                        }
                    }
                    //As with patient, creating with PUT would not be idempotent or make us use ids provided by client
                    and("Doctor doesn't exist") {
                        coEvery { doctorRepository.existsById(validId) } returns false
                        then("Return NotFound") {
                            webTestClient.put().uri("/doctors/$validId").bodyValue(validBody).exchange()
                                .expectStatus().isNotFound

                            coVerify(exactly = 1) { doctorRepository.existsById(validId) }
                            coVerify(inverse = true) { doctorRepository.save(any()) }
                        }
                    }
                }
                and("Invalid Body") {
                    val invalidBody = mapOf("firstName" to validBody.firstName, "lastName" to validBody.lastName)
                    then("Bad Request, DB untouched") {
                        webTestClient.put().uri("/doctors/$validId").bodyValue(invalidBody).exchange()
                            .expectStatus().isBadRequest

                        verify { doctorRepository wasNot called }
                    }
                }
            }
            `when`("Invalid Id and valid body") {
                val invalidId = Arb.continent().single().toString()
                then("Bad Request, DB untouched") {
                    webTestClient.put().uri("/doctors/$invalidId").bodyValue(validBody).exchange()
                        .expectStatus().isBadRequest

                    verify { doctorRepository wasNot called }
                }
            }
        }
    }
}
