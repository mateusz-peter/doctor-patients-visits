package dev.mtpeter.rsqrecruitmenttask.patient

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface PatientRepository : CoroutineCrudRepository<Patient, Long>