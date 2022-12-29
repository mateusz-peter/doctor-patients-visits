package dev.mtpeter.rsqrecruitmenttask.configuration

import dev.mtpeter.rsqrecruitmenttask.doctor.Doctor
import dev.mtpeter.rsqrecruitmenttask.patient.Patient
import dev.mtpeter.rsqrecruitmenttask.visit.Visit
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.IsolationMode
import io.kotest.extensions.spring.SpringExtension
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.arbs.color
import io.kotest.property.arbs.firstName
import io.kotest.property.arbs.geo.country
import io.kotest.property.arbs.lastName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.reactor.asCoroutineContext
import kotlinx.coroutines.withContext
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import reactor.util.context.Context

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
}

fun postgresContainer(tenantId: String) = PostgreSQLContainer<Nothing>("postgres:15.1-alpine").apply {
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

suspend fun <T> withTenant(
    tenantId: String,
    block: suspend CoroutineScope.() -> T
) = withContext(Context.of("TenantId", tenantId).asCoroutineContext(), block)

val patientArb = arbitrary {
    val firstName = Arb.firstName().bind().toString()
    val lastName = Arb.lastName().bind().toString()
    val address = Arb.country().bind().toString()
    Patient(null, firstName, lastName, address)
}

val doctorArb = arbitrary {
    val firstName = Arb.firstName().bind().name
    val lastName = Arb.lastName().bind().name
    val specialty = Arb.color().bind().value
    Doctor(null, firstName, lastName, specialty)
}

val visitArb = arbitrary {
    val visitDate = Arb.localDate().single()
    val visitTime = Arb.localTime().single().withSecond(0).withNano(0)
    val place = Arb.country().single().name
    val doctorId = Arb.long(10L..20L).single()
    val patientId = Arb.long(10L..20L).single()
    Visit(null, visitDate, visitTime, place, doctorId, patientId)
}