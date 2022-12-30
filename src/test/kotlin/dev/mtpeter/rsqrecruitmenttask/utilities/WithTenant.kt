package dev.mtpeter.rsqrecruitmenttask.utilities

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.reactor.asCoroutineContext
import kotlinx.coroutines.withContext
import reactor.util.context.Context

suspend fun <T> withTenant(
    tenantId: String,
    block: suspend CoroutineScope.() -> T
) = withContext(Context.of("TenantId", tenantId).asCoroutineContext(), block)