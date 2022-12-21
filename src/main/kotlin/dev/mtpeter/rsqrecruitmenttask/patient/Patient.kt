package dev.mtpeter.rsqrecruitmenttask.patient

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("patient")
data class Patient(
    @Id val id: Long?,
    @Column("first_name") val firstName: String,
    @Column("last_name") val lastName: String,
    val address: String
)

data class PatientDTO(
    val firstName: String,
    val lastName: String,
    val address: String
) {
    fun toPatient(id: Long? = null) = Patient(id, firstName, lastName, address)
}