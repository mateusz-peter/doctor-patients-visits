package dev.mtpeter.rsqrecruitmenttask.configuration

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse

@Component
class TenantAwareRoutingDummy : TenantAwareRouting(TenantProperties(emptyMap())) {
    override suspend fun tenantAwareFilter(
        serverRequest: ServerRequest,
        next: suspend (ServerRequest) -> ServerResponse
    ): ServerResponse = next(serverRequest)
}