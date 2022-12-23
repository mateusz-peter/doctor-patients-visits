package dev.mtpeter.rsqrecruitmenttask.visit

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface VisitRepository : CoroutineCrudRepository<Visit, Long> {
    fun findBy(pageable: Pageable): Flow<Visit>
    fun findByPatientId(patientId: Long, pageable: Pageable): Flow<Visit>
    suspend fun countByPatientId(patientId: Long): Long
}