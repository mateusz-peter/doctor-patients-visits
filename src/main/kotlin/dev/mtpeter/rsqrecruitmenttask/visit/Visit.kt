package dev.mtpeter.rsqrecruitmenttask.visit

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.LocalTime

@Table("visit")
data class Visit(
    @Id val id: Long?,
    @Column("visit_date") val visitDate: LocalDate,
    @Column("visit_hour") val visitTime: LocalTime,
    val place: String,
    @Column("doctor_id") val doctorId: Long,
    @Column("patient_id") val patientId: Long
)

data class VisitDTO(
    val visitDate: LocalDate,
    val visitTime: LocalTime,
    val place: String,
    val doctorId: Long,
    val patientId: Long
) {
    fun toVisit(id: Long? = null) = Visit(id, visitDate, visitTime, place, doctorId, patientId)
    fun validated(): VisitDTO? = if (visitTime.second == 0 && visitTime.nano == 0) this else null
}

fun Visit.toDTO() = VisitDTO(visitDate, visitTime, place, doctorId, patientId)