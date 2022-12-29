package dev.mtpeter.rsqrecruitmenttask.doctor

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("doctor")
data class Doctor(
    @Id val id: Long?,
    @Column("first_name") val firstName: String,
    @Column("last_name") val lastName: String,
    val specialty: String
)

data class DoctorDTO(
    val firstName: String,
    val lastName: String,
    val specialty: String
) {
    fun toDoctor(id: Long? = null) = Doctor(id, firstName, lastName, specialty)
}
fun Doctor.toDTO() = DoctorDTO(firstName, lastName, specialty)