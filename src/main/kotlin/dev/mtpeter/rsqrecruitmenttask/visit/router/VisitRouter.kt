package dev.mtpeter.rsqrecruitmenttask.visit.router

import dev.mtpeter.rsqrecruitmenttask.multitenancy.TenantAwareRouting
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class VisitRouter {

    @Bean
    fun routeVisits(
        visitHandler: VisitHandler,
        tenantAwareRouting: TenantAwareRouting
    ) = coRouter {
        GET("/visits", visitHandler::getAllVisits)
        GET("/visits/paged", visitHandler::getVisitsPaged)
        POST("/visits", visitHandler::scheduleVisit)
        PUT("/visits/{id}", visitHandler::rescheduleVisit)
        filter(tenantAwareRouting::tenantAwareFilter)
    }
}