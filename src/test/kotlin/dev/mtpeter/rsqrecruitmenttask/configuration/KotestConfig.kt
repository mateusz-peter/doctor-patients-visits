package dev.mtpeter.rsqrecruitmenttask.configuration

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.IsolationMode
import io.kotest.extensions.spring.SpringExtension
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy

const val TENANT_A = "tenantA"
const val TENANT_B = "tenantB"

class KotestConfig : AbstractProjectConfig() {

    override val isolationMode: IsolationMode
        get() = IsolationMode.InstancePerLeaf

    override fun extensions(): List<Extension> = listOf(SpringExtension)

    override suspend fun beforeProject() {
        postgresContainer(TENANT_A)
        postgresContainer(TENANT_B)
    }

    private fun postgresContainer(tenantId: String) = PostgreSQLContainer<Nothing>("postgres:15.1-alpine").apply {
        startupAttempts = 1
        withInitScript("dbSchema.sql")
        withUsername("test")
        withPassword("test")
        withDatabaseName(tenantId)
        start()
        waitingFor(HostPortWaitStrategy())

        System.setProperty("dev.mtpeter.rsq.tenants.$tenantId.username", this.username)
        System.setProperty("dev.mtpeter.rsq.tenants.$tenantId.password", this.password)
        System.setProperty(
            "dev.mtpeter.rsq.tenants.$tenantId.url",
            "r2dbc:postgresql://$host:${getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)}/$databaseName"
        )
    }
}