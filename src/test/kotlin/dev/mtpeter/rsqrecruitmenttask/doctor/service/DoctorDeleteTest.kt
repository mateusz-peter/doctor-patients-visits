package dev.mtpeter.rsqrecruitmenttask.doctor.service

import dev.mtpeter.rsqrecruitmenttask.configuration.doctorArb
import dev.mtpeter.rsqrecruitmenttask.doctor.DoctorRepository
import dev.mtpeter.rsqrecruitmenttask.doctor.router.DeletedDoctor
import dev.mtpeter.rsqrecruitmenttask.doctor.router.DoctorHasVisits
import dev.mtpeter.rsqrecruitmenttask.doctor.router.DoctorNotFound
import dev.mtpeter.rsqrecruitmenttask.doctor.router.DoctorService
import dev.mtpeter.rsqrecruitmenttask.visit.VisitRepository
import io.kotest.core.spec.style.behaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.positiveLong
import io.kotest.property.arbitrary.single
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.runs

fun doctorDeleteTest(doctorRepository: DoctorRepository, visitRepository: VisitRepository, doctorService: DoctorService) = behaviorSpec {
    val doctorId = Arb.positiveLong().single()
    given("A doctor with given {id} in DB") {
        val doctor = doctorArb.single().copy(id = doctorId)
        coEvery { doctorRepository.findById(doctorId) } returns doctor

        and("The doctor has visits") {
            coEvery { visitRepository.existsByDoctorId(doctorId) } returns true
            coEvery { visitRepository.removeByDoctorId(doctorId) } just runs

            `when`("deleting a doctor") {
                and("cascade=true") {
                    coEvery { doctorRepository.deleteById(doctorId) } just runs

                    then("Delete visits and doctor; Return DeletedDoctor") {
                        doctorService.deleteDoctor(doctorId, true) shouldBe DeletedDoctor(doctor)

                        coVerify(exactly = 1) { doctorRepository.findById(doctorId) }
                        coVerify(exactly = 1) { visitRepository.removeByDoctorId(doctorId) }
                        coVerify(exactly = 1) { doctorRepository.deleteById(doctorId) }
                    }
                }
                and("cascade=false") {
                    then("Don't delete; Return DoctorHasVisits") {
                        doctorService.deleteDoctor(doctorId, false) shouldBe DoctorHasVisits

                        coVerify(exactly = 1) { doctorRepository.findById(doctorId) }
                        coVerify(exactly = 1) { visitRepository.existsByDoctorId(doctorId) }
                        coVerify(inverse = true) { doctorRepository.deleteById(any()) }
                    }
                }
            }
        }
        and("The doctor doesn't have visits") {
            coEvery { visitRepository.existsByDoctorId(doctorId) } returns false
            coEvery { visitRepository.removeByDoctorId(doctorId) } just runs

            `when`("deleting a doctor") {
                coEvery { doctorRepository.deleteById(doctorId) } just runs

                listOf(true, false).forEach { cascade ->
                    and("cascade=$cascade") {
                        then("Delete a doctor; Return DeletedDoctor") {
                            doctorService.deleteDoctor(doctorId, cascade) shouldBe DeletedDoctor(doctor)

                            coVerify(exactly = 1) { doctorRepository.deleteById(doctorId) }
                        }
                    }
                }
            }
        }
    }
    given("No doctor with given {id} in DB") {
        coEvery { doctorRepository.findById(doctorId) } returns null
        `when`("deleting a doctor") {
            listOf(true, false).forEach { cascade ->
                and("cascade=$cascade") {
                    then("Return DoctorNotFound") {
                        doctorService.deleteDoctor(doctorId, cascade) shouldBe DoctorNotFound

                        coVerify(exactly = 1) { doctorRepository.findById(doctorId) }
                    }
                }
            }
        }
    }
}