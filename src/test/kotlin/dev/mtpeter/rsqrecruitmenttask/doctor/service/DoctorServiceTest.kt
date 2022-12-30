package dev.mtpeter.rsqrecruitmenttask.doctor.service

import dev.mtpeter.rsqrecruitmenttask.doctor.DoctorRepository
import dev.mtpeter.rsqrecruitmenttask.doctor.router.DoctorService
import dev.mtpeter.rsqrecruitmenttask.visit.VisitRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.mockk


class DoctorServiceTest : BehaviorSpec() {

    private val doctorRepository = mockk<DoctorRepository>()
    private val visitRepository = mockk<VisitRepository>()
    private val doctorService = DoctorService(doctorRepository, visitRepository)

    init {
        include(doctorFindTest(doctorRepository, doctorService))
        include(doctorDeleteTest(doctorRepository, visitRepository, doctorService))
    }
}