package dev.mtpeter.rsqrecruitmenttask.patient.router

import dev.mtpeter.rsqrecruitmenttask.configuration.TenantAwareRouting
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.coRouter

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