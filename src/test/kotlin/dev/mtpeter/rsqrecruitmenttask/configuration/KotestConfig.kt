package dev.mtpeter.rsqrecruitmenttask.configuration

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.IsolationMode
import io.kotest.extensions.spring.SpringExtension
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.MountableFile

class KotestConfig : AbstractProjectConfig() {

    override val isolationMode: IsolationMode
        get() = IsolationMode.InstancePerLeaf

    override fun extensions(): List<Extension> = listOf(SpringExtension)

    override suspend fun beforeProject() {
        super.beforeProject()

        PostgreSQLContainer<Nothing>("postgres:15.1-alpine").run {
            startupAttempts = 1
            withCopyToContainer(
                MountableFile.forClasspathResource("dbSchema.sql"),
                "/docker-entrypoint-initdb.d/init.sql"
            )
            withUsername("test")
            withPassword("test")
            withDatabaseName("tenantA")
            start()
            waitingFor(HostPortWaitStrategy())

            System.setProperty("dev.mtpeter.rsq.tenants.tenantA.username", this.username)
            System.setProperty("dev.mtpeter.rsq.tenants.tenantA.password", this.password)
            System.setProperty(
                "dev.mtpeter.rsq.tenants.tenantA.url",
                "r2dbc:postgresql://$host:${getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)}/$databaseName"
            )
        }
    }
}