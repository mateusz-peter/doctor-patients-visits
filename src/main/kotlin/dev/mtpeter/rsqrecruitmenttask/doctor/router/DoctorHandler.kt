package dev.mtpeter.rsqrecruitmenttask.doctor.router

import dev.mtpeter.rsqrecruitmenttask.doctor.DoctorDTO
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*

@Component
class DoctorHandler(
    private val doctorService: DoctorService
) {

    suspend fun getAllDoctors(serverRequest: ServerRequest): ServerResponse {
        return ServerResponse.ok().bodyAndAwait(doctorService.getAllDoctors())
    }

    suspend fun getPagedDoctors(serverRequest: ServerRequest): ServerResponse {
        val pageNo = serverRequest.queryParamOrNull("page")?.toIntOrNull() ?: 0
        val pageSize = serverRequest.queryParamOrNull("size")?.toIntOrNull() ?: 10
        val sort = Sort.by("lastName", "firstName")

        return ServerResponse.ok().bodyValueAndAwait(doctorService.getPagedDoctors(pageNo, pageSize, sort))
    }

    suspend fun getDoctorById(serverRequest: ServerRequest): ServerResponse {
        val id = serverRequest.pathVariable("id").toLongOrNull() ?: return ServerResponse.badRequest().buildAndAwait()

        val doctor = doctorService.getDoctorById(id) ?: return ServerResponse.notFound().buildAndAwait()
        return ServerResponse.ok().bodyValueAndAwait(doctor)
    }

    suspend fun createDoctor(serverRequest: ServerRequest): ServerResponse {
        val body = serverRequest.awaitBodyOrNull<DoctorDTO>() ?: return ServerResponse.badRequest().buildAndAwait()
        val saved = doctorService.createDoctor(body)
        val location = serverRequest.uriBuilder().path("/${saved.id}").build()
        return ServerResponse.created(location).bodyValueAndAwait(saved)
    }

    suspend fun updateDoctor(serverRequest: ServerRequest): ServerResponse {
        val body = serverRequest.awaitBodyOrNull<DoctorDTO>() ?: return ServerResponse.badRequest().buildAndAwait()
        val id = serverRequest.pathVariable("id").toLongOrNull() ?: return ServerResponse.badRequest().buildAndAwait()

        val saved = doctorService.updateDoctor(id, body) ?: return ServerResponse.notFound().buildAndAwait()
        return ServerResponse.ok().bodyValueAndAwait(saved)
    }

    suspend fun deleteDoctor(serverRequest: ServerRequest): ServerResponse {
        val id = serverRequest.pathVariable("id").toLongOrNull() ?: return ServerResponse.badRequest().buildAndAwait()
        val cascade = serverRequest.queryParamOrNull("cascade")?.toBoolean() ?: false

        return when(val result = doctorService.deleteDoctor(id, cascade)) {
            is DoctorNotFound -> ServerResponse.notFound().buildAndAwait()
            is DoctorHasVisits -> ServerResponse.status(HttpStatus.CONFLICT).buildAndAwait()
            is DeletedDoctor -> ServerResponse.ok().bodyValueAndAwait(result.doctor)
        }
    }
}