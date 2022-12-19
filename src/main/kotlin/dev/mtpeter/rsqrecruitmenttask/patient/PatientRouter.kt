package dev.mtpeter.rsqrecruitmenttask.patient

import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*

@Component
class PatientRouter(
    private val patientHandler: PatientHandler
) {

    @Bean
    fun router() = coRouter {
        GET("/patients", patientHandler::getAllPatients)
        GET("/patients/{id}", patientHandler::getPatientById)
    }
}

@Component
class PatientHandler(
    private val patientRepository: PatientRepository
) {

    suspend fun getAllPatients(request: ServerRequest): ServerResponse {
        val patientFlow = patientRepository.findAll()
        return ServerResponse.ok().bodyAndAwait(patientFlow)
    }

    suspend fun getPatientById(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id").toLongOrNull() ?: return ServerResponse.badRequest().buildAndAwait()
        val patient = patientRepository.findById(id) ?: return ServerResponse.notFound().buildAndAwait()

        return ServerResponse.ok().bodyValueAndAwait(patient)
    }
}