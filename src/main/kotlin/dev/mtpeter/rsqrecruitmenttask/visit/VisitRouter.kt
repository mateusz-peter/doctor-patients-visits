package dev.mtpeter.rsqrecruitmenttask.visit

import dev.mtpeter.rsqrecruitmenttask.configuration.TenantAwareRouting
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class VisitRouter {

    @Bean
    fun routeVisits(
        visitHandler: VisitHandler,
        tenantAwareRouting: TenantAwareRouting
    ) = coRouter {
        GET("/visits") {
            ok().buildAndAwait()
        }

        filter(tenantAwareRouting::tenantAwareFilter)
    }
}

@Component
class VisitHandler(
    private val visitService: VisitService
) {

}

@Component
class VisitService(
    private val visitRepository: VisitRepository
) {

}