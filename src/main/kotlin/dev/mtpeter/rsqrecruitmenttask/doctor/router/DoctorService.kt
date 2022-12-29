package dev.mtpeter.rsqrecruitmenttask.doctor.router

import dev.mtpeter.rsqrecruitmenttask.doctor.Doctor
import dev.mtpeter.rsqrecruitmenttask.doctor.DoctorDTO
import dev.mtpeter.rsqrecruitmenttask.doctor.DoctorRepository
import dev.mtpeter.rsqrecruitmenttask.visit.VisitRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DoctorService(
    private val doctorRepository: DoctorRepository,
    private val visitRepository: VisitRepository
) {
    fun getAllDoctors(): Flow<Doctor> = doctorRepository.findAll()

    @Transactional(readOnly = true)
    suspend fun getPagedDoctors(pageNo: Int, pageSize: Int, sort: Sort): Page<Doctor> = coroutineScope {
        val pageRequest = PageRequest.of(pageNo, pageSize, sort)
        val pageContent = async { doctorRepository.findAllBy(pageRequest).toList() }
        val total = async { doctorRepository.count() }

        PageImpl(pageContent.await(), pageRequest, total.await())
    }

    suspend fun getDoctorById(id: Long): Doctor? = doctorRepository.findById(id)

    suspend fun createDoctor(doctorDTO: DoctorDTO): Doctor = doctorRepository.save(doctorDTO.toDoctor())

    @Transactional
    suspend fun updateDoctor(id: Long, doctorDTO: DoctorDTO): Doctor? {
        val exists = doctorRepository.existsById(id)
        if(!exists) return null

        return doctorRepository.save(doctorDTO.toDoctor(id))
    }

    @Transactional
    suspend fun deleteDoctor(id: Long, cascade: Boolean): DoctorRemovalResult {
        val docToDelete = doctorRepository.findById(id) ?: return DoctorNotFound
        if(!cascade && visitRepository.existsByDoctorId(id))
            return DoctorHasVisits
        if(cascade)
            visitRepository.removeByDoctorId(id)

        doctorRepository.deleteById(id)
        return DeletedDoctor(docToDelete)
    }
}

sealed interface DoctorRemovalResult
object DoctorNotFound: DoctorRemovalResult
object DoctorHasVisits: DoctorRemovalResult
data class DeletedDoctor(val doctor: Doctor): DoctorRemovalResult