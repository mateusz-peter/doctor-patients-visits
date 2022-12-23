package dev.mtpeter.rsqrecruitmenttask.visit

import dev.mtpeter.rsqrecruitmenttask.patient.Patient
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface VisitRepository : CoroutineCrudRepository<Visit, Long> {
    fun findByPatientId(patientId: Long, pageable: Pageable): Flow<Patient>
}