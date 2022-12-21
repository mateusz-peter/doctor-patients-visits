package dev.mtpeter.rsqrecruitmenttask.patient

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface PatientRepository : CoroutineCrudRepository<Patient, Long> {
    fun findAllBy(pageable: Pageable): Flow<Patient>
}