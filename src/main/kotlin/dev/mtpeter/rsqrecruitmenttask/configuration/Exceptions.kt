package dev.mtpeter.rsqrecruitmenttask.configuration

class NoTenantException : RuntimeException()
class InvalidTenantException(val tenantId: String) : RuntimeException()