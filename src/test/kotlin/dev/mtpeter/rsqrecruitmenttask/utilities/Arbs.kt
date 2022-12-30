package dev.mtpeter.rsqrecruitmenttask.utilities

import dev.mtpeter.rsqrecruitmenttask.doctor.Doctor
import dev.mtpeter.rsqrecruitmenttask.patient.Patient
import dev.mtpeter.rsqrecruitmenttask.visit.Visit
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.arbs.color
import io.kotest.property.arbs.firstName
import io.kotest.property.arbs.geo.country
import io.kotest.property.arbs.lastName
import org.springframework.data.domain.PageRequest

val patientArb = arbitrary {
    val firstName = Arb.firstName().bind().name
    val lastName = Arb.lastName().bind().name
    val address = Arb.country().bind().name
    Patient(null, firstName, lastName, address)
}
val doctorArb = arbitrary {
    val firstName = Arb.firstName().bind().name
    val lastName = Arb.lastName().bind().name
    val specialty = Arb.color().bind().value
    Doctor(null, firstName, lastName, specialty)
}
val visitArb = arbitrary {
    val visitDate = Arb.localDate().bind()
    val visitTime = Arb.localTime().bind().withSecond(0).withNano(0)
    val place = Arb.country().bind().name
    val doctorId = Arb.long(10L..20L).bind()
    val patientId = Arb.long(10L..20L).bind()
    Visit(null, visitDate, visitTime, place, doctorId, patientId)
}

fun pageRequestArb(total: Int) = arbitrary {
    val pageSize = Arb.positiveInt().bind()
    val totalPages = total / pageSize + if (total % pageSize > 0) 1 else 0
    val pageNo = Arb.int(0 until totalPages).bind()
    PageRequest.of(pageNo, pageSize)
}