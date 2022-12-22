package dev.mtpeter.rsqrecruitmenttask.doctor

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class DoctorRouter {

    @Bean
    fun routeDoctors(
        doctorRepository: DoctorRepository
    ) = coRouter {
        GET("/doctors") {
            ok().buildAndAwait()
        }
    }
}

