package dev.mtpeter.rsqrecruitmenttask.multitenancy

class NoTenantException : RuntimeException()
class InvalidTenantException(val tenantId: String) : RuntimeException()