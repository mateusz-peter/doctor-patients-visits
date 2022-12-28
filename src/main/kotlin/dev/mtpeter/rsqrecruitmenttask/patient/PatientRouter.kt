package dev.mtpeter.rsqrecruitmenttask.patient

import dev.mtpeter.rsqrecruitmenttask.configuration.TenantAwareRouting
import dev.mtpeter.rsqrecruitmenttask.visit.VisitRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
    private val patientRepository: PatientRepository,
    private val visitRepository: VisitRepository
) {

    suspend fun getAllPatientsPaged(request: ServerRequest): ServerResponse = coroutineScope {
        val pageNo = request.queryParamOrNull("page")?.toIntOrNull() ?: 0
        val pageSize = request.queryParamOrNull("size")?.toIntOrNull() ?: 10
        val sort = Sort.by("lastName").and(Sort.by("firstName"))
        val pageRequest = PageRequest.of(pageNo, pageSize, sort)

        val patients = async { patientRepository.findAllBy(pageRequest).toList() }
        val total = async { patientRepository.count() }

        val page = PageImpl(patients.await(), pageRequest, total.await())
        ServerResponse.ok().bodyValueAndAwait(page)
    }

    suspend fun getAllPatients(request: ServerRequest): ServerResponse {
        val patientFlow = patientRepository.findAll()
        return ServerResponse.ok().bodyAndAwait(patientFlow)
    }

    suspend fun getPatientById(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id").toLongOrNull() ?: return ServerResponse.badRequest().buildAndAwait()
        val patient = patientRepository.findById(id) ?: return ServerResponse.notFound().buildAndAwait()

        return ServerResponse.ok().bodyValueAndAwait(patient)
    }

    suspend fun saveNewPatient(request: ServerRequest): ServerResponse {
        val patientDTO = request.awaitBody<PatientDTO>()
        val saved = patientRepository.save(patientDTO.toPatient())

        return ServerResponse.created(request.uriBuilder().path("/${saved.id}").build())
            .bodyValueAndAwait(saved)
    }

    suspend fun updatePatient(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id").toLongOrNull() ?: return ServerResponse.badRequest().buildAndAwait()
        val patientDTO = request.awaitBody<PatientDTO>()

        val exists = patientRepository.existsById(id)
        if(!exists) return ServerResponse.notFound().buildAndAwait()

        val saved = patientRepository.save(patientDTO.toPatient(id))
        return ServerResponse.ok().bodyValueAndAwait(saved)
    }

    suspend fun deletePatient(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id").toLongOrNull() ?: return ServerResponse.badRequest().buildAndAwait()
        val cascade = request.queryParamOrNull("cascade")?.toBoolean() ?: false

        if(!patientRepository.existsById(id))
            return ServerResponse.notFound().buildAndAwait()
        if(!cascade && visitRepository.existsByPatientId(id))
            return ServerResponse.status(HttpStatus.CONFLICT).buildAndAwait()
        if(cascade)
            visitRepository.removeByPatientId(id)

        patientRepository.deleteById(id)
        return ServerResponse.noContent().buildAndAwait()
    }
}