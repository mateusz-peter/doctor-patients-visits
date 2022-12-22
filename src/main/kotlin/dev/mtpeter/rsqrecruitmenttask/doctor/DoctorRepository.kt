package dev.mtpeter.rsqrecruitmenttask.doctor

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface DoctorRepository : CoroutineCrudRepository<Doctor, Long> {
    fun findAllBy(pageable: Pageable): Flow<Doctor>
}