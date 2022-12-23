package dev.mtpeter.rsqrecruitmenttask.visit

import dev.mtpeter.rsqrecruitmenttask.configuration.TenantAwareRouting
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*

@Configuration
class VisitRouter {

    @Bean
    fun routeVisits(
        visitHandler: VisitHandler,
        tenantAwareRouting: TenantAwareRouting
    ) = coRouter {
        GET("/visits", visitHandler::getAllVisits)
        GET("/visits/paged", visitHandler::getVisitsPaged)
        POST("/visits", visitHandler::scheduleVisit)
        filter(tenantAwareRouting::tenantAwareFilter)
    }
}

@Component
class VisitHandler(
    private val visitService: VisitService
) {
    suspend fun getAllVisits(serverRequest: ServerRequest): ServerResponse {
        return ServerResponse.ok().bodyAndAwait(visitService.getAllVisits())
    }

    suspend fun getVisitsPaged(serverRequest: ServerRequest): ServerResponse {
        val pageNo = serverRequest.queryParamOrNull("page")?.toIntOrNull() ?: 0
        val pageSize = serverRequest.queryParamOrNull("size")?.toIntOrNull() ?: 10
        val sort = Sort.by("visitDate", "visitTime").descending()
        val patientId = serverRequest.queryParamOrNull("id")?.toLong()

        val page = visitService.getVisitsPaged(pageNo, pageSize, sort, patientId)
        return ServerResponse.ok().bodyValueAndAwait(page)
    }

    suspend fun scheduleVisit(serverRequest: ServerRequest): ServerResponse {
        val body = serverRequest.awaitBodyOrNull<VisitDTO>() ?: return ServerResponse.badRequest().buildAndAwait()
        val saved = visitService.scheduleVisit(body.toVisit()) ?: return ServerResponse.status(HttpStatus.CONFLICT).buildAndAwait()
        val location = serverRequest.uriBuilder().path("/${saved.id}").build()
        return ServerResponse.created(location).bodyValueAndAwait(saved)
    }
}

@Component
class VisitService(
    private val visitRepository: VisitRepository
) {
    fun getAllVisits(): Flow<Visit> = visitRepository.findAll()

    suspend fun getVisitsPaged(pageNo: Int, pageSize: Int, sort: Sort, patientId: Long?): Page<Visit> = coroutineScope {
        val pageRequest = PageRequest.of(pageNo, pageSize, sort)
        val visitFlow =
            if (patientId == null) visitRepository.findBy(pageRequest)
            else visitRepository.findByPatientId(patientId, pageRequest)
        val visits = async { visitFlow.toList() }
        val total = async {
            if (patientId == null) visitRepository.count()
            else visitRepository.countByPatientId(patientId)
        }

        PageImpl(visits.await(), pageRequest, total.await())
    }

    suspend fun scheduleVisit(visit: Visit): Visit? {
        val conflictingVisit = visitRepository.findByVisitDateAndVisitTimeAndDoctorId(visit.visitDate, visit.visitTime, visit.doctorId)
        if (conflictingVisit != null) return null

        return visitRepository.save(visit)
    }
}