package dev.mtpeter.rsqrecruitmenttask.configuration

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class TenantFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val tenantId = exchange.request.headers["X-TenantID"].orEmpty().singleOrNull() ?: return chain.filter(exchange)
        return chain.filter(exchange)
            .contextWrite { it.put("TenantId", tenantId) }
    }
}

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

