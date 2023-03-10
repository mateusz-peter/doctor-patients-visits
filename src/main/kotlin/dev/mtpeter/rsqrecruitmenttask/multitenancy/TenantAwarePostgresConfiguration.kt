package dev.mtpeter.rsqrecruitmenttask.multitenancy

import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryMetadata
import org.springframework.boot.r2dbc.ConnectionFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.r2dbc.connection.lookup.AbstractRoutingConnectionFactory
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Configuration
@EnableR2dbcRepositories
@EnableTransactionManagement
class TenantAwarePostgresConfiguration(
    tenantProperties: TenantProperties
) : AbstractR2dbcConfiguration() {

    val tenants = tenantProperties.tenants

    @Bean
    fun transactionalOperator(transactionManager: R2dbcTransactionManager) = TransactionalOperator.create(transactionManager)

    @Bean
    fun transactionManager(connectionFactory: ConnectionFactory) = R2dbcTransactionManager(connectionFactory)

    @Bean
    override fun connectionFactory(): ConnectionFactory {
        return makeConnectionFactory().apply {
            afterPropertiesSet()
        }
    }

    private fun makeConnectionFactory() = TenantAwareConnectionFactory(tenants.keys).apply {
        setLenientFallback(false)
        setTargetConnectionFactories(tenantsFromConfiguration())
    }

    private fun tenantsFromConfiguration(): Map<String, ConnectionFactory> = tenants.mapValues { (_, connection) ->
        ConnectionFactoryBuilder.withUrl(connection.url)
            .username(connection.username)
            .password(connection.password)
            .build()
    }
}

private class TenantAwareConnectionFactory(
    val validTenants: Set<String>
) : AbstractRoutingConnectionFactory() {

    override fun getMetadata(): ConnectionFactoryMetadata = ConnectionFactoryMetadata { "PostgreSQL" }

    override fun determineCurrentLookupKey(): Mono<Any> {
        return Mono.deferContextual { it.toMono() }
            .filter { it.hasKey("TenantId") }
            .map { it.get<String>("TenantId") }
            .switchIfEmpty(Mono.defer { NoTenantException().toMono() })
            .flatMap {
                if (it in validTenants) it.toMono()
                else Mono.defer { InvalidTenantException(it).toMono() }
            }
    }
}