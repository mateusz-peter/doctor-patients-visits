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
    @Column("visit_time") val visitTime: LocalTime,
    val place: String,
    @Column("doctor_id") val doctorId: Long,
    @Column("patient_id") val patientId: Long
)
