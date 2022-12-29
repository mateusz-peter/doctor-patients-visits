package dev.mtpeter.rsqrecruitmenttask.patient.router

import dev.mtpeter.rsqrecruitmenttask.patient.Patient
import dev.mtpeter.rsqrecruitmenttask.patient.PatientDTO
import dev.mtpeter.rsqrecruitmenttask.patient.PatientRepository
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
class PatientService(
    private val patientRepository: PatientRepository,
    private val visitRepository: VisitRepository
) {

    fun getAllPatients(): Flow<Patient> = patientRepository.findAll()

    suspend fun getPatientById(id: Long) = patientRepository.findById(id)

    @Transactional(readOnly = true)
    suspend fun getPagedPatients(pageNo: Int, pageSize: Int, sort: Sort): Page<Patient> = coroutineScope {
        val pageRequest = PageRequest.of(pageNo, pageSize, sort)

        val patients = async { patientRepository.findAllBy(pageRequest).toList() }
        val total = async { patientRepository.count() }

        PageImpl(patients.await(), pageRequest, total.await())
    }

    suspend fun saveNewPatient(patientDTO: PatientDTO): Patient = patientRepository.save(patientDTO.toPatient())

    @Transactional
    suspend fun updatePatient(id: Long, patientDTO: PatientDTO): Patient? {
        if(!patientRepository.existsById(id)) return null
        return patientRepository.save(patientDTO.toPatient(id))
    }

    @Transactional
    suspend fun deletePatient(id: Long, cascade: Boolean): PatientRemovalResult {
        if (!patientRepository.existsById(id)) return PatientNotFound
        if (!cascade && visitRepository.existsByPatientId(id)) return PatientHasVisits
        if (cascade) visitRepository.removeByPatientId(id)

        patientRepository.deleteById(id)
        return DeletedSuccess
    }
}

sealed interface PatientRemovalResult
object PatientNotFound : PatientRemovalResult
object PatientHasVisits : PatientRemovalResult
object DeletedSuccess : PatientRemovalResult