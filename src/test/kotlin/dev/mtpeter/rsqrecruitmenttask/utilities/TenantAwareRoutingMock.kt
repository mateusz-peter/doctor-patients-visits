package dev.mtpeter.rsqrecruitmenttask.utilities

import dev.mtpeter.rsqrecruitmenttask.multitenancy.TenantAwareRouting
import dev.mtpeter.rsqrecruitmenttask.multitenancy.TenantProperties
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse

@Component
class TenantAwareRoutingMock : TenantAwareRouting(TenantProperties(emptyMap())) {
    override suspend fun tenantAwareFilter(
        serverRequest: ServerRequest,
        next: suspend (ServerRequest) -> ServerResponse
    ): ServerResponse = next(serverRequest)
}