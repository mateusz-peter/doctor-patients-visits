package dev.mtpeter.rsqrecruitmenttask.doctor

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("doctor")
data class Doctor(
    @Id val id: Long?,
    val firstName: String,
    val lastName: String,
    val specialty: String
)

