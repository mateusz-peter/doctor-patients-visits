package dev.mtpeter.rsqrecruitmenttask

import dev.mtpeter.rsqrecruitmenttask.configuration.TenantProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(value = [TenantProperties::class])
class RsqRecruitmentTaskApplication

fun main(args: Array<String>) {
	runApplication<RsqRecruitmentTaskApplication>(*args)
}
