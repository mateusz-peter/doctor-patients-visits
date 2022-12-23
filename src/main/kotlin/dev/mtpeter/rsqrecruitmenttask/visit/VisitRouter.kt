package dev.mtpeter.rsqrecruitmenttask.visit

import dev.mtpeter.rsqrecruitmenttask.configuration.TenantAwareRouting
import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyAndAwait
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class VisitRouter {

    @Bean
    fun routeVisits(
        visitHandler: VisitHandler,
        tenantAwareRouting: TenantAwareRouting
    ) = coRouter {
        GET("/visits", visitHandler::getAllVisits)

        filter(tenantAwareRouting::tenantAwareFilter)
    }
}

@Component
class VisitHandler(
    private val visitService: VisitService
) {
    suspend fun getAllVisits(serverRequest: ServerRequest): ServerResponse {
        return ServerResponse.ok().bodyAndAwait(visitService.getAllVisits())
    }
}

@Component
class VisitService(
    private val visitRepository: VisitRepository
) {
    fun getAllVisits(): Flow<Visit> = visitRepository.findAll()
}