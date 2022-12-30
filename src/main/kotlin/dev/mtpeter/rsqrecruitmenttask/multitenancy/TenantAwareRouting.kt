package dev.mtpeter.rsqrecruitmenttask.multitenancy

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.buildAndAwait

@Component
class TenantAwareRouting(
    val tenantProperties: TenantProperties
) {

    suspend fun tenantAwareFilter(
        serverRequest: ServerRequest,
        next: suspend (ServerRequest) -> ServerResponse
    ): ServerResponse {
        val tenantId = serverRequest.headers().header("X-TenantID")
            .singleOrNull() ?: return ServerResponse.badRequest().buildAndAwait()
        if (tenantId !in tenantProperties.tenants.keys) return ServerResponse.badRequest().buildAndAwait()

        return next(serverRequest)
    }
}