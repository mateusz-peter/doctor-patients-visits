package dev.mtpeter.rsqrecruitmenttask.multitenancy

import org.springframework.stereotype.Component
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