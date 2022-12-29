package dev.mtpeter.rsqrecruitmenttask.doctor.router

import dev.mtpeter.rsqrecruitmenttask.configuration.TenantAwareRouting
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class DoctorRouter {

    @Bean
    fun routeDoctors(
        doctorHandler: DoctorHandler,
        tenantAwareRouting: TenantAwareRouting
    ) = coRouter {
        GET("/doctors", doctorHandler::getAllDoctors)
        GET("/doctors/paged", doctorHandler::getPagedDoctors)
        GET("/doctors/{id}", doctorHandler::getDoctorById)
        POST("/doctors", doctorHandler::createDoctor)
        PUT("/doctors/{id}", doctorHandler::updateDoctor)
        DELETE("/doctors/{id}", doctorHandler::deleteDoctor)

        filter(tenantAwareRouting::tenantAwareFilter)
    }
}

