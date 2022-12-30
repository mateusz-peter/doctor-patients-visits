package dev.mtpeter.rsqrecruitmenttask.multitenancy

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding


@ConfigurationProperties(prefix = "dev.mtpeter.rsq")
data class TenantProperties @ConstructorBinding constructor(
    val tenants: Map<String, TenantConnection>
) {

    data class TenantConnection @ConstructorBinding constructor(
        val username: String,
        val password: String,
        val url: String
    )
}
