package dev.mtpeter.rsqrecruitmenttask.visit.router

import dev.mtpeter.rsqrecruitmenttask.visit.VisitDTO
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*

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

    suspend fun cancelVisit(serverRequest: ServerRequest): ServerResponse {
        val id = serverRequest.pathVariable("id").toLongOrNull() ?: return ServerResponse.badRequest().buildAndAwait()

        return if (visitService.cancelVisit(id))
            ServerResponse.noContent().buildAndAwait()
        else
            ServerResponse.notFound().buildAndAwait()
    }

    suspend fun scheduleVisit(serverRequest: ServerRequest): ServerResponse {
        val body =
            serverRequest.awaitBodyOrNull<VisitDTO>()?.validated() ?: return ServerResponse.badRequest().buildAndAwait()
        val saved = visitService.scheduleVisit(body.toVisit()) ?: return ServerResponse.status(HttpStatus.CONFLICT)
            .buildAndAwait()
        val location = serverRequest.uriBuilder().path("/${saved.id}").build()
        return ServerResponse.created(location).bodyValueAndAwait(saved)
    }

    suspend fun rescheduleVisit(serverRequest: ServerRequest): ServerResponse {
        val id = serverRequest.pathVariable("id").toLongOrNull() ?: return ServerResponse.badRequest().buildAndAwait()
        val body =
            serverRequest.awaitBodyOrNull<VisitDTO>()?.validated() ?: return ServerResponse.badRequest().buildAndAwait()

        return when (val result = visitService.rescheduleVisit(body.toVisit(id))) {
            is ExistingVisitNotFound -> ServerResponse.notFound().buildAndAwait()
            is TryingToChangePatient -> ServerResponse.badRequest().buildAndAwait()
            is ConflictingVisit -> ServerResponse.status(HttpStatus.CONFLICT).buildAndAwait()
            is SavedVisit -> ServerResponse.ok().bodyValueAndAwait(result.visit)
        }
    }
}