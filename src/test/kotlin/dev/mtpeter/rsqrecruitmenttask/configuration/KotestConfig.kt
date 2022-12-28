package dev.mtpeter.rsqrecruitmenttask.configuration

import dev.mtpeter.rsqrecruitmenttask.patient.Patient
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.IsolationMode
import io.kotest.extensions.spring.SpringExtension
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbs.firstName
import io.kotest.property.arbs.geo.country
import io.kotest.property.arbs.lastName
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

val patientArb = arbitrary {
    val firstName = Arb.firstName().bind().toString()
    val lastName = Arb.lastName().bind().toString()
    val address = Arb.country().bind().toString()
    Patient(null, firstName, lastName, address)
}