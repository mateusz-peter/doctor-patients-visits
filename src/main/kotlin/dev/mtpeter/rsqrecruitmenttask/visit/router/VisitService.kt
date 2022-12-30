package dev.mtpeter.rsqrecruitmenttask.visit.router

import dev.mtpeter.rsqrecruitmenttask.visit.Visit
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
class VisitService(
    private val visitRepository: VisitRepository
) {

    fun getAllVisits(): Flow<Visit> = visitRepository.findAll()

    @Transactional(readOnly = true)
    suspend fun getVisitsPaged(pageNo: Int, pageSize: Int, sort: Sort, patientId: Long?): Page<Visit> = coroutineScope {
        val pageRequest = PageRequest.of(pageNo, pageSize, sort)
        val visitFlow =
            if (patientId == null) visitRepository.findBy(pageRequest)
            else visitRepository.findByPatientId(patientId, pageRequest)
        val visits = async { visitFlow.toList() }
        val total = async {
            if (patientId == null) visitRepository.count()
            else visitRepository.countByPatientId(patientId)
        }

        PageImpl(visits.await(), pageRequest, total.await())
    }

    @Transactional
    suspend fun cancelVisit(id: Long): Boolean {
        if (!visitRepository.existsById(id)) return false
        visitRepository.deleteById(id)
        return true
    }

    @Transactional
    suspend fun scheduleVisit(visit: Visit): Visit? {
        val conflictingVisit =
            visitRepository.findByVisitDateAndVisitTimeAndDoctorId(visit.visitDate, visit.visitTime, visit.doctorId)
        if (conflictingVisit != null) return null

        return visitRepository.save(visit)
    }

    @Transactional
    suspend fun rescheduleVisit(visitToUpdate: Visit): RescheduleResult {
        val existingVisit = visitRepository.findById(visitToUpdate.id!!) ?: return ExistingVisitNotFound
        if (visitToUpdate.patientId != existingVisit.patientId) return TryingToChangePatient
        val conflictingVisit = visitRepository.findByVisitDateAndVisitTimeAndDoctorId(
            visitToUpdate.visitDate,
            visitToUpdate.visitTime,
            visitToUpdate.doctorId
        )
        if (conflictingVisit != null) return ConflictingVisit
        val saved = visitRepository.save(visitToUpdate)
        return SavedVisit(saved)
    }
}

sealed interface RescheduleResult
object ExistingVisitNotFound : RescheduleResult
object TryingToChangePatient : RescheduleResult
object ConflictingVisit : RescheduleResult
data class SavedVisit(val visit: Visit) : RescheduleResult