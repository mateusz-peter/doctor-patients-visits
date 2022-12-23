package dev.mtpeter.rsqrecruitmenttask.doctor

import dev.mtpeter.rsqrecruitmenttask.configuration.TenantAwareRouting
import dev.mtpeter.rsqrecruitmenttask.visit.VisitRepository
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
class DoctorRouter {

    @Bean
    fun routeDoctors(
        doctorHandler: DoctorHandler,
        tenantAwareRouting: TenantAwareRouting
    ) = coRouter {
        GET("/doctors", doctorHandler::getAllDoctors)
        GET("/doctors/paged", doctorHandler::getPagedDoctors)
        GET("/doctors/{id}", doctorHandler::getDoctorById)
        POST("/doctors", doctorHandler::createDoctor)
        PUT("/doctors/{id}", doctorHandler::updateDoctor)
        DELETE("/doctors/{id}", doctorHandler::deleteDoctor)

        filter(tenantAwareRouting::tenantAwareFilter)
    }
}

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
        return when(val result = doctorService.deleteDoctor(id)) {
            is DoctorNotFound -> ServerResponse.notFound().buildAndAwait()
            is DoctorHasVisits -> ServerResponse.status(HttpStatus.CONFLICT).buildAndAwait()
            is DeletedDoctor -> ServerResponse.ok().bodyValueAndAwait(result.doctor)
        }
    }
}

@Component
class DoctorService(
    private val doctorRepository: DoctorRepository,
    private val visitRepository: VisitRepository
) {
    fun getAllDoctors(): Flow<Doctor> = doctorRepository.findAll()

    suspend fun getPagedDoctors(pageNo: Int, pageSize: Int, sort: Sort): Page<Doctor> = coroutineScope {
        val pageRequest = PageRequest.of(pageNo, pageSize, sort)
        val pageContent = async { doctorRepository.findAllBy(pageRequest).toList() }
        val total = async { doctorRepository.count() }

        PageImpl(pageContent.await(), pageRequest, total.await())
    }

    suspend fun getDoctorById(id: Long): Doctor? = doctorRepository.findById(id)

    suspend fun createDoctor(doctorDTO: DoctorDTO): Doctor = doctorRepository.save(doctorDTO.toDoctor())

    suspend fun updateDoctor(id: Long, doctorDTO: DoctorDTO): Doctor? {
        val exists = doctorRepository.existsById(id)
        if(!exists) return null

        return doctorRepository.save(doctorDTO.toDoctor())
    }

    suspend fun deleteDoctor(id: Long): RemovalResult {
        val docToDelete = doctorRepository.findById(id) ?: return DoctorNotFound
        if (visitRepository.existsByDoctorId(id)) return DoctorHasVisits

        doctorRepository.deleteById(id)
        return DeletedDoctor(docToDelete)
    }
}

sealed interface RemovalResult
object DoctorNotFound: RemovalResult
object DoctorHasVisits: RemovalResult
data class DeletedDoctor(val doctor: Doctor): RemovalResult
