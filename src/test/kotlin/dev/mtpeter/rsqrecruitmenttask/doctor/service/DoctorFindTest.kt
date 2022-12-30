package dev.mtpeter.rsqrecruitmenttask.doctor.service

import dev.mtpeter.rsqrecruitmenttask.utilities.doctorArb
import dev.mtpeter.rsqrecruitmenttask.utilities.pageRequestArb
import dev.mtpeter.rsqrecruitmenttask.doctor.DoctorRepository
import dev.mtpeter.rsqrecruitmenttask.doctor.router.DoctorService
import io.kotest.core.spec.style.behaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.single
import io.kotest.property.arbitrary.take
import io.kotest.property.checkAll
import io.kotest.property.forAll
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

fun doctorFindTest(doctorRepository: DoctorRepository, doctorService: DoctorService) = behaviorSpec {
    given("n doctors in DB") {
        val n = Arb.int(1000..2000).single()
        val doctors = doctorArb.take(n).mapIndexed { i, d -> d.copy(id = i.toLong()) }.toList()
        every { doctorRepository.findAll() } returns doctors.asFlow()
        coEvery { doctorRepository.findById(any()) } coAnswers {
            val id = firstArg<Long>()
            doctors.getOrNull(id.toInt())
        }
        coEvery { doctorRepository.findAllBy(any()) } coAnswers {
            val pageable = firstArg<Pageable>()
            doctors.drop(pageable.pageNumber*pageable.pageSize).take(pageable.pageSize).asFlow()
        }
        coEvery { doctorRepository.count() } returns n.toLong()

        `when`("Getting all doctors") {
            then("All doctors are returned") {
                doctorService.getAllDoctors().toList() shouldBe doctors
            }
        }
        `when`("Getting a doctor with random id") {
            and("provided existing id") {
                then("The doctor is returned") {
                    forAll(Arb.long(0L until n.toLong())) { id ->
                        doctorService.getDoctorById(id) == doctors[id.toInt()]
                    }
                }
            }
            and("provided non-existing id") {
                then("null is returned") {
                    forAll(Arb.long(min = n.toLong())) { id ->
                        doctorService.getDoctorById(id) == null
                    }
                }
            }
        }
        `when`("Getting paged doctors") {
            and("provided page and size") {
                then("proper page is returned") {
                    checkAll(pageRequestArb(n)) {
                        val page = doctorService.getPagedDoctors(it.pageNumber, it.pageSize, Sort.unsorted())
                        page.totalPages shouldBe n / it.pageSize + if (n % it.pageSize > 0) 1 else 0
                        page.number shouldBe it.pageNumber
                        if(page.isLast && page.size > 1) {
                            page.numberOfElements shouldBe n % it.pageSize
                        } else {
                            page.numberOfElements shouldBe it.pageSize
                        }
                        page.content shouldBe doctors.drop(it.pageNumber*it.pageSize).take(it.pageSize)
                    }
                }
            }
        }
    }
}