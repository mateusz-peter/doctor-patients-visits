package dev.mtpeter.rsqrecruitmenttask.patient.router

import dev.mtpeter.rsqrecruitmenttask.patient.PatientDTO
import kotlinx.coroutines.coroutineScope
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.server.*

@Transactional
@Component
class PatientHandler(
    private val patientService: PatientService
) {

    suspend fun getAllPatientsPaged(request: ServerRequest): ServerResponse = coroutineScope {
        val pageNo = request.queryParamOrNull("page")?.toIntOrNull() ?: 0
        val pageSize = request.queryParamOrNull("size")?.toIntOrNull() ?: 10
        val sort = Sort.by("lastName").and(Sort.by("firstName"))

        val page = patientService.getPagedPatients(pageNo, pageSize, sort)
        ServerResponse.ok().bodyValueAndAwait(page)
    }

    suspend fun getAllPatients(request: ServerRequest): ServerResponse {
        val patientFlow = patientService.getAllPatients()
        return ServerResponse.ok().bodyAndAwait(patientFlow)
    }

    suspend fun getPatientById(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id").toLongOrNull() ?: return ServerResponse.badRequest().buildAndAwait()

        val patient = patientService.getPatientById(id) ?: return ServerResponse.notFound().buildAndAwait()
        return ServerResponse.ok().bodyValueAndAwait(patient)
    }

    suspend fun saveNewPatient(request: ServerRequest): ServerResponse {
        val patientDTO = request.awaitBody<PatientDTO>()
        val saved = patientService.saveNewPatient(patientDTO)

        return ServerResponse.created(request.uriBuilder().path("/${saved.id}").build())
            .bodyValueAndAwait(saved)
    }

    suspend fun updatePatient(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id").toLongOrNull() ?: return ServerResponse.badRequest().buildAndAwait()
        val patientDTO = request.awaitBody<PatientDTO>()

        val saved = patientService.updatePatient(id, patientDTO) ?: return ServerResponse.notFound().buildAndAwait()
        return ServerResponse.ok().bodyValueAndAwait(saved)
    }

    suspend fun deletePatient(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id").toLongOrNull() ?: return ServerResponse.badRequest().buildAndAwait()
        val cascade = request.queryParamOrNull("cascade")?.toBoolean() ?: false

        return when(patientService.deletePatient(id, cascade)) {
            is PatientNotFound -> ServerResponse.notFound().buildAndAwait()
            is PatientHasVisits -> ServerResponse.status(HttpStatus.CONFLICT).buildAndAwait()
            is DeletedSuccess -> ServerResponse.noContent().buildAndAwait()
        }
    }
}