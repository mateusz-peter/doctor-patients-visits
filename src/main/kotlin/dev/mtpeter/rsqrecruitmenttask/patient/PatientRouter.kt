package dev.mtpeter.rsqrecruitmenttask.patient

import dev.mtpeter.rsqrecruitmenttask.configuration.TenantAwareRouting
import dev.mtpeter.rsqrecruitmenttask.visit.VisitRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.server.*

@Configuration
class PatientRouter {

    @Bean
    fun routePatients(
        patientHandler: PatientHandler,
        tenantAwareRouting: TenantAwareRouting
    ) = coRouter {
        GET("/patients", patientHandler::getAllPatients)
        GET("/patients/paged", patientHandler::getAllPatientsPaged)
        GET("/patients/{id}", patientHandler::getPatientById)
        POST("/patients", patientHandler::saveNewPatient)
        PUT("/patients/{id}", patientHandler::updatePatient)
        DELETE("/patients/{id}", patientHandler::deletePatient)
        filter(tenantAwareRouting::tenantAwareFilter)
    }
}

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

@Component
class PatientService(
    private val patientRepository: PatientRepository,
    private val visitRepository: VisitRepository
) {

    fun getAllPatients(): Flow<Patient> = patientRepository.findAll()

    suspend fun getPatientById(id: Long) = patientRepository.findById(id)

    @Transactional
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
    suspend fun deletePatient(id: Long, cascade: Boolean): RemovalResult {
        if (!patientRepository.existsById(id)) return PatientNotFound
        if (!cascade && visitRepository.existsByPatientId(id)) return PatientHasVisits
        if (cascade) visitRepository.removeByPatientId(id)

        patientRepository.deleteById(id)
        return DeletedSuccess
    }
}

sealed interface RemovalResult
object PatientNotFound : RemovalResult
object PatientHasVisits : RemovalResult
object DeletedSuccess : RemovalResult