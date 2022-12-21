package dev.mtpeter.rsqrecruitmenttask.patient

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*

@Configuration
class PatientRouter {

    @Bean
    fun router(
        patientHandler: PatientHandler
    ) = coRouter {
        GET("/patients", patientHandler::getAllPatients)
        GET("/patients/{id}", patientHandler::getPatientById)
        POST("/patients", patientHandler::saveNewPatient)
        PUT("/patients/{id}", patientHandler::updatePatient)
        DELETE("/patients/{id}", patientHandler::deletePatient)
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

        val exists = patientRepository.existsById(id)
        if(!exists) return ServerResponse.notFound().buildAndAwait()

        patientRepository.deleteById(id)
        return ServerResponse.ok().buildAndAwait()
    }
}